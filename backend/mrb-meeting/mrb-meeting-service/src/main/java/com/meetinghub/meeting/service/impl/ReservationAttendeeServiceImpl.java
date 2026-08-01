package com.meetinghub.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meetinghub.common.constant.DateTimePatternConstant;
import com.meetinghub.meeting.api.enums.AttendeeStatusEnum;
import com.meetinghub.meeting.api.enums.ReservationStatusEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.platform.api.model.dto.NotificationSendDTO;
import com.meetinghub.user.api.feign.UserFeignClient;
import com.meetinghub.user.api.model.dto.UserBriefDTO;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.entity.ReservationAttendee;
import com.meetinghub.meeting.model.vo.AttendeeVO;
import com.meetinghub.platform.api.mq.producer.NotificationSender;
import com.meetinghub.meeting.repository.ReservationAttendeeRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.service.ReservationAttendeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.Collator;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预约参会人服务实现
 * <p>
 * 通过 Feign 跨服务查询 mrb-user 用户信息并回填到 {@link AttendeeVO}。
 * 邀请逻辑采用「追加式」：已存在的参会人跳过，不重复插入。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationAttendeeServiceImpl
        extends ServiceImpl<ReservationAttendeeRepository, ReservationAttendee>
        implements ReservationAttendeeService {

    private final ReservationAttendeeRepository attendeeRepository;
    private final ReservationRepository reservationRepository;
    private final UserFeignClient userFeignClient;
    private final NotificationSender notificationSender;

    /** 中文拼音排序器（Collator 非线程安全，用 ThreadLocal 隔离） */
    private static final ThreadLocal<Collator> ZH_COLLATOR =
            ThreadLocal.withInitial(() -> Collator.getInstance(Locale.CHINA));

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int inviteAttendees(Long reservationId, Long inviterId, List<Long> userIds) {
        if (reservationId == null || inviterId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "预约ID或邀请人ID不能为空");
        }
        if (CollectionUtils.isEmpty(userIds)) {
            return 0;
        }

        // 校验预约存在且归属邀请人
        checkReservationOwnership(reservationId, inviterId);

        // 状态约束：仅 PENDING / CONFIRMED 可邀人（已取消/已拒绝/已结束不可邀）
        MeetingRoomReservation reservation = reservationRepository.selectById(reservationId);
        Integer status = reservation.getStatus();
        if (!ReservationStatusEnum.PENDING.getCode().equals(status)
                && !ReservationStatusEnum.CONFIRMED.getCode().equals(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "预约已取消或已拒绝，无法邀请参会人");
        }
        if (ReservationStatusEnum.CONFIRMED.getCode().equals(status)
                && reservation.getEndTime() != null
                && reservation.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "会议已结束，无法邀请参会人");
        }

        // 查询已存在的参会人，去重
        List<ReservationAttendee> existing = attendeeRepository.selectList(
                new LambdaQueryWrapper<ReservationAttendee>()
                        .eq(ReservationAttendee::getReservationId, reservationId)
                        .in(ReservationAttendee::getUserId, userIds)
        );
        Set<Long> existingUserIds = existing.stream()
                .map(ReservationAttendee::getUserId)
                .collect(Collectors.toSet());

        // 过滤掉已存在和自己（邀请人默认是组织者，不重复加入参会人列表）
        List<Long> toAdd = userIds.stream()
                .filter(uid -> !existingUserIds.contains(uid))
                .filter(uid -> !uid.equals(inviterId))
                .distinct()
                .toList();
        if (toAdd.isEmpty()) {
            return 0;
        }

        List<ReservationAttendee> records = toAdd.stream().map(uid -> {
            ReservationAttendee a = new ReservationAttendee();
            a.setReservationId(reservationId);
            a.setUserId(uid);
            a.setStatus(AttendeeStatusEnum.PENDING.getCode());
            return a;
        }).collect(Collectors.toList());

        // 批量插入（IService.saveBatch 默认 1000 条一批）
        saveBatch(records);
        log.info("邀请参会人, reservationId={}, inviterId={}, added={}", reservationId, inviterId, records.size());

        // 修改预约的参会人数
        reservation.setAttendeeCount(reservation.getAttendeeCount() + toAdd.size());
        reservationRepository.updateById(reservation);

        // 通知参会人：仅当免审批（立即确认）时才在此处通知；
        // 需审批的预约在 approveReservation 中审批通过后通知参会人
        if (reservation.getStatus().equals(ReservationStatusEnum.CONFIRMED.getCode())) {
            NotificationSendDTO notify = new NotificationSendDTO();
            notify.setType("RESERVATION_CREATED");
            notify.setTitle("您被邀请参加会议：" + reservation.getSubject());
            notify.setContent("会议主题：" + reservation.getSubject() + "\n预约编号：" + reservation.getReservationCode()
                    + "\n时间：" + reservation.getStartTime().format(DateTimePatternConstant.DATETIME_FMT)
                    + " ~ " + reservation.getEndTime().format(DateTimePatternConstant.DATETIME_FMT));
            notify.setRefType("reservation");
            notify.setRefId(reservation.getId());
            notificationSender.sendSafe(toAdd, notify);
        }
        return records.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int inviteDepartment(Long reservationId, Long inviterId, Long departmentId) {
        if (departmentId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "部门ID不能为空");
        }
        // 通过 Feign 查询部门所有成员
        List<UserBriefDTO> members;
        try {
            var result = userFeignClient.listByDepartment(departmentId);
            if (result == null || result.getData() == null) {
                return 0;
            }
            members = result.getData();
        } catch (Exception e) {
            log.error("Feign 调用 mrb-user 查询部门成员失败, departmentId={}", departmentId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "查询部门成员失败");
        }
        if (members.isEmpty()) {
            return 0;
        }
        List<Long> userIds = members.stream()
                .map(UserBriefDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return inviteAttendees(reservationId, inviterId, userIds);
    }

    @Override
    public List<AttendeeVO> listAttendees(Long reservationId) {
        if (reservationId == null) {
            return Collections.emptyList();
        }
        List<ReservationAttendee> attendees = attendeeRepository.selectList(
                new LambdaQueryWrapper<ReservationAttendee>()
                        .eq(ReservationAttendee::getReservationId, reservationId)
                        .orderByAsc(ReservationAttendee::getCreateTime)
        );
        if (attendees.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量 Feign 拉取用户信息
        List<Long> userIds = attendees.stream()
                .map(ReservationAttendee::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UserBriefDTO> userMap = batchQueryUsers(userIds);

        return attendees.stream()
                .map(a -> {
                    AttendeeVO vo = new AttendeeVO();
                    vo.setUserId(a.getUserId());
                    vo.setStatus(a.getStatus());
                    vo.setCreateTime(a.getCreateTime());
                    UserBriefDTO u = userMap.get(a.getUserId());
                    if (u != null) {
                        vo.setUsername(u.getUsername());
                        vo.setRealName(u.getRealName());
                        vo.setPhone(u.getPhone());
                        vo.setEmail(u.getEmail());
                        vo.setDepartmentId(u.getDepartmentId());
                        vo.setDepartmentName(u.getDepartmentName());
                        vo.setAvatar(u.getAvatar());
                    }
                    return vo;
                })
                // 二次排序：先按通知时间（createTime）升序，相同时按真实姓名拼音升序（空值居后）
                .sorted(Comparator
                        .comparing(AttendeeVO::getCreateTime,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AttendeeVO::getRealName,
                                Comparator.nullsLast(ZH_COLLATOR.get())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAttendee(Long reservationId, Long userId, Long operatorId) {
        if (reservationId == null || userId == null || operatorId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "参数不能为空");
        }
        // 校验操作权限：仅预约创建者可移除参会人
        checkReservationOwnership(reservationId, operatorId);

        int deleted = attendeeRepository.delete(
                new LambdaQueryWrapper<ReservationAttendee>()
                        .eq(ReservationAttendee::getReservationId, reservationId)
                        .eq(ReservationAttendee::getUserId, userId)
        );
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.ATTENDEE_NOT_INVITED);
        }
    }

    /**
     * 校验预约存在且归属当前用户
     */
    private void checkReservationOwnership(Long reservationId, Long operatorId) {
        MeetingRoomReservation reservation = reservationRepository.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        if (!reservation.getUserId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.RESERVATION_ACCESS_DENIED);
        }
    }

    /**
     * 批量查询用户信息，失败时降级返回空 Map
     */
    private Map<Long, UserBriefDTO> batchQueryUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            var result = userFeignClient.listByIds(userIds);
            if (result == null || result.getData() == null) {
                return Collections.emptyMap();
            }
            return result.getData().stream()
                    .filter(u -> u.getId() != null)
                    .collect(Collectors.toMap(UserBriefDTO::getId, u -> u, (a, b) -> a));
        } catch (Exception e) {
            log.error("Feign 批量查询用户失败, userIds={}", userIds, e);
            return Collections.emptyMap();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCreatorAsAttendee(Long reservationId, Long userId) {
        // 检查是否已存在（避免重复加入）
        Long existCount = attendeeRepository.selectCount(
                new LambdaQueryWrapper<ReservationAttendee>()
                        .eq(ReservationAttendee::getReservationId, reservationId)
                        .eq(ReservationAttendee::getUserId, userId)
        );
        if (existCount > 0) {
            return;
        }
        ReservationAttendee attendee = new ReservationAttendee();
        attendee.setReservationId(reservationId);
        attendee.setUserId(userId);
        attendee.setStatus(1); // 已接受
        attendeeRepository.insert(attendee);
    }

    @Override
    public boolean isAttendee(Long reservationId, Long userId) {
        if (reservationId == null || userId == null) {
            return false;
        }
        return attendeeRepository.selectCount(
                new LambdaQueryWrapper<ReservationAttendee>()
                        .eq(ReservationAttendee::getReservationId, reservationId)
                        .eq(ReservationAttendee::getUserId, userId)
        ) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAttendeeStatus(Long reservationId, Long userId, Integer status) {
        if (reservationId == null || userId == null || status == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "参数不能为空");
        }
        // 仅允许更新为已接受(1)或已拒绝(2)
        if (!status.equals(AttendeeStatusEnum.ACCEPTED.getCode())
                && !status.equals(AttendeeStatusEnum.DECLINED.getCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "无效的参会状态");
        }
        ReservationAttendee attendee = attendeeRepository.selectOne(
                new LambdaQueryWrapper<ReservationAttendee>()
                        .eq(ReservationAttendee::getReservationId, reservationId)
                        .eq(ReservationAttendee::getUserId, userId)
        );
        if (attendee == null) {
            throw new BusinessException(ErrorCode.ATTENDEE_NOT_INVITED);
        }
        // 已是目标状态则跳过
        if (attendee.getStatus() != null && attendee.getStatus().equals(status)) {
            return;
        }
        attendee.setStatus(status);
        attendeeRepository.updateById(attendee);
        log.info("参会人状态更新, reservationId={}, userId={}, status={}", reservationId, userId, status);
    }
}
