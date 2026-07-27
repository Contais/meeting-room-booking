package com.meetinghub.meeting.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.enums.ReservationStatusEnum;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约状态闭环定时任务
 * <p>
 * 处理"已到预约时段但仍未审批通过"的场景：
 * 每分钟扫描 PENDING 且 start_time < now 的预约，CAS 更新为 REJECTED，
 * 同时填充拒绝原因，释放会议室时段，避免被永久占用。
 * </p>
 * <p>
 * 幂等性：CAS 更新 WHERE status=PENDING，重复扫描无副作用。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduleTask {

    private static final String AUTO_REJECT_REASON = "超时未审批，系统自动拒绝";

    private final ReservationRepository reservationRepository;

    /**
     * 每分钟扫描超时未审批的预约
     */
    @Scheduled(cron = "0 * * * * ?")
    public void handleExpiredPendingReservations() {
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
        for (MeetingRoomReservation r : expired) {
            try {
                MeetingRoomReservation update = new MeetingRoomReservation();
                update.setId(r.getId());
                update.setStatus(ReservationStatusEnum.REJECTED.getCode());
                update.setRejectReason(AUTO_REJECT_REASON);
                // CAS 更新：WHERE status=PENDING，避免与并发审批操作冲突
                int rows = reservationRepository.update(update,
                        new LambdaQueryWrapper<MeetingRoomReservation>()
                                .eq(MeetingRoomReservation::getId, r.getId())
                                .eq(MeetingRoomReservation::getStatus, ReservationStatusEnum.PENDING.getCode())
                );
                if (rows > 0) {
                    log.info("[ReservationScheduleTask] 预约 {} 已自动拒绝", r.getId());
                }
            } catch (Exception e) {
                log.error("[ReservationScheduleTask] 自动拒绝预约 {} 失败", r.getId(), e);
            }
        }
    }
}
