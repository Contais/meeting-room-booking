package com.meetinghub.meeting.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.constant.DateTimePatternConstant;
import com.meetinghub.common.constant.RedisKeyConstant;
import com.meetinghub.meeting.api.enums.ReservationStatusEnum;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.vo.AttendeeVO;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.service.ReservationAttendeeService;
import com.meetinghub.platform.api.model.dto.NotificationSendDTO;
import com.meetinghub.platform.api.mq.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 预约状态闭环 + 提醒定时任务
 * <p>
 * 1. {@link #handleExpiredPendingReservations()}：每分钟扫描超时未审批的预约，CAS 自动拒绝，释放会议室时段。
 * 2. {@link #remindUpcomingReservations()}：每分钟扫描 15 分钟内即将开始的已确认预约，发送站内信提醒。
 * </p>
 * <p>
 * 分布式锁：Redis {@code mrb:schedule:lock:{task}} 防多实例重复执行（当前单实例部署，锁作为防护兜底）。
 * 幂等性：CAS 更新 + Redis 已提醒标记，重复扫描无副作用。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduleTask {

    private static final String AUTO_REJECT_REASON = "超时未审批，系统自动拒绝";
    private static final long LOCK_TTL_SECONDS = 55L;
    private static final long REMIND_BEFORE_MINUTES = 15L;
    private static final long REMIND_FLAG_TTL_HOURS = 2L;

    private final ReservationRepository reservationRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final ReservationAttendeeService attendeeService;
    private final NotificationProducer notificationProducer;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 每分钟扫描超时未审批的预约（已到开始时间但仍为 PENDING）
     */
    @Scheduled(cron = "0 * * * * ?")
    public void handleExpiredPendingReservations() {
        String lockId = tryAcquireLock("expired-pending");
        if (lockId == null) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            List<MeetingRoomReservation> expired = reservationRepository.selectList(
                    new LambdaQueryWrapper<MeetingRoomReservation>()
                            .eq(MeetingRoomReservation::getStatus, ReservationStatusEnum.PENDING.getCode())
                            .lt(MeetingRoomReservation::getStartTime, now)
            );
            if (expired.isEmpty()) {
                return;
            }
            log.info("[ReservationScheduleTask] 发现 {} 条超时未审批预约，开始自动拒绝", expired.size());
            for (MeetingRoomReservation reservation : expired) {
                try {
                    MeetingRoomReservation update = new MeetingRoomReservation();
                    update.setId(reservation.getId());
                    update.setStatus(ReservationStatusEnum.REJECTED.getCode());
                    update.setRejectReason(AUTO_REJECT_REASON);
                    // CAS 更新：WHERE status=PENDING，避免与并发审批操作冲突
                    int rows = reservationRepository.update(update,
                            new LambdaQueryWrapper<MeetingRoomReservation>()
                                    .eq(MeetingRoomReservation::getId, reservation.getId())
                                    .eq(MeetingRoomReservation::getStatus, ReservationStatusEnum.PENDING.getCode())
                    );
                    if (rows > 0) {
                        log.info("[ReservationScheduleTask] 预约 {} 已自动拒绝", reservation.getId());
                    }
                } catch (Exception e) {
                    log.error("[ReservationScheduleTask] 自动拒绝预约 {} 失败", reservation.getId(), e);
                }
            }
        } finally {
            releaseLock("expired-pending", lockId);
        }
    }

    /**
     * 每分钟扫描 15 分钟内即将开始的已确认预约，发送站内信提醒
     */
    @Scheduled(cron = "0 * * * * ?")
    public void remindUpcomingReservations() {
        String lockId = tryAcquireLock("upcoming-remind");
        if (lockId == null) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime windowEnd = now.plusMinutes(REMIND_BEFORE_MINUTES);
            List<MeetingRoomReservation> upcoming = reservationRepository.selectList(
                    new LambdaQueryWrapper<MeetingRoomReservation>()
                            .eq(MeetingRoomReservation::getStatus, ReservationStatusEnum.CONFIRMED.getCode())
                            .ge(MeetingRoomReservation::getStartTime, now)
                            .le(MeetingRoomReservation::getStartTime, windowEnd)
            );
            if (upcoming.isEmpty()) {
                return;
            }
            for (MeetingRoomReservation r : upcoming) {
                try {
                    remindOnce(r);
                } catch (Exception e) {
                    log.error("[ReservationScheduleTask] 预约 {} 提醒发送失败", r.getId(), e);
                }
            }
        } finally {
            releaseLock("upcoming-remind", lockId);
        }
    }

    /**
     * 对单条预约发送提醒（幂等：Redis 已提醒标记防重复）
     */
    private void remindOnce(MeetingRoomReservation r) {
        String flagKey = RedisKeyConstant.SCHEDULE_REMINDED + r.getId();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(flagKey, "1", Duration.ofHours(REMIND_FLAG_TTL_HOURS));
        if (Boolean.FALSE.equals(acquired)) {
            return;
        }
        List<Long> userIds = attendeeService.listAttendees(r.getId()).stream()
                .map(AttendeeVO::getUserId).collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        String roomName = resolveRoomName(r.getRoomId());
        NotificationSendDTO notify = new NotificationSendDTO();
        notify.setType("RESERVATION_REMINDER");
        notify.setTitle("会议即将开始：" + r.getSubject());
        notify.setContent("会议主题：" + r.getSubject()
                + "\n预约编号：" + r.getReservationCode()
                + "\n时间：" + r.getStartTime().format(DateTimePatternConstant.DATETIME_FMT) + " ~ " + r.getEndTime().format(DateTimePatternConstant.DATETIME_FMT)
                + (roomName != null ? "\n会议室：" + roomName : ""));
        notify.setRefType("reservation");
        notify.setRefId(r.getId());
        try {
            notificationProducer.send(userIds, notify);
            log.info("[ReservationScheduleTask] 预约 {} 即将开始提醒已发送, 接收人数={}", r.getId(), userIds.size());
        } catch (Exception e) {
            // MQ 投递失败：释放提醒标记，下轮扫描重试
            stringRedisTemplate.delete(flagKey);
            log.warn("[ReservationScheduleTask] 预约 {} 提醒 MQ 投递失败，已释放标记等待重试", r.getId(), e);
        }
    }

    private String resolveRoomName(Long roomId) {
        if (roomId == null) {
            return null;
        }
        MeetingRoom room = meetingRoomRepository.selectById(roomId);
        return room != null ? room.getName() : null;
    }

    /**
     * 抢占分布式锁（Redis setIfAbsent + TTL），返回锁持有者标识用于安全释放
     */
    private String tryAcquireLock(String taskName) {
        String lockKey = RedisKeyConstant.SCHEDULE_LOCK + taskName;
        String instanceId = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, instanceId, Duration.ofSeconds(LOCK_TTL_SECONDS));
        return Boolean.TRUE.equals(acquired) ? instanceId : null;
    }

    /**
     * 释放分布式锁：仅当锁值仍为当前实例标识时才删除（Lua 原子操作，避免误删其他实例的锁）
     */
    private void releaseLock(String taskName, String instanceId) {
        if (instanceId == null) {
            return;
        }
        String lockKey = RedisKeyConstant.SCHEDULE_LOCK + taskName;
        // KEYS[1]=lockKey ARGV[1]=instanceId：仅当值匹配时删除
        String lua = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        org.springframework.data.redis.core.script.DefaultRedisScript<Long> script = new org.springframework.data.redis.core.script.DefaultRedisScript<>(lua, Long.class);
        stringRedisTemplate.execute(script, java.util.List.of(lockKey), instanceId);
    }
}
