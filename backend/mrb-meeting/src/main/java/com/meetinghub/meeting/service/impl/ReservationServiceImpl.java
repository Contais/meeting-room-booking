package com.meetinghub.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.enums.ApprovalModeEnum;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.enums.ReservationStatusEnum;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.meeting.model.dto.ReservationCreateDTO;
import com.meetinghub.meeting.model.dto.ReservationPageQuery;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.feign.UserFeignClient;
import com.meetinghub.meeting.model.vo.ReservationVO;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.service.ReservationAttendeeService;
import com.meetinghub.meeting.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import com.meetinghub.meeting.model.vo.ScheduleReservationVO;
import com.meetinghub.meeting.model.vo.ScheduleRoomVO;
import com.meetinghub.meeting.model.vo.ScheduleVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预约服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl extends ServiceImpl<ReservationRepository, MeetingRoomReservation> implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final UserFeignClient userFeignClient;
    private final ReservationAttendeeService attendeeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createReservation(Long userId, ReservationCreateDTO dto) {
        // 1. 校验会议室存在
        MeetingRoom room = meetingRoomRepository.selectById(dto.getRoomId());
        if (room == null) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }
        if (room.getStatus().equals(EnableStatusEnum.DISABLED.getCode())) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_DISABLED);
        }

        // 2. 校验时间合法性
        if (dto.getEndTime().isBefore(dto.getStartTime()) || dto.getEndTime().isEqual(dto.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "结束时间必须晚于开始时间");
        }

        // 3. 使用规则校验
        validateRoomRules(room, dto);

        // 4. 冲突检测
        boolean hasConflict = checkTimeConflict(dto.getRoomId(), dto.getStartTime(), dto.getEndTime(), null);
        if (hasConflict) {
            throw new BusinessException(ErrorCode.RESERVATION_CONFLICT);
        }

        // 5. 创建预约（根据审批模式设置初始状态）
        MeetingRoomReservation reservation = new MeetingRoomReservation();
        reservation.setRoomId(dto.getRoomId());
        reservation.setUserId(userId);
        reservation.setSubject(dto.getSubject());
        int attendeeCount = dto.getAttendeeUserIds() != null ? dto.getAttendeeUserIds().size() : 0;
        reservation.setAttendeeCount(attendeeCount);
        reservation.setRemark(dto.getRemark());
        reservation.setStartTime(dto.getStartTime());
        reservation.setEndTime(dto.getEndTime());
        Integer initialStatus = room.getNeedApproval().equals(ApprovalModeEnum.NEED_APPROVAL.getCode())
                ? ReservationStatusEnum.PENDING.getCode()
                : ReservationStatusEnum.CONFIRMED.getCode();
        reservation.setStatus(initialStatus);
        save(reservation);

        // 6. 生成预约编号：B + yyyyMMdd + 6位自增序列（基于主键 id，保证唯一）
        String reservationCode = generateReservationCode(reservation.getId());
        reservation.setReservationCode(reservationCode);
        updateById(reservation);
        log.info("预约创建成功, userId={}, roomId={}, code={}, status={}", userId, dto.getRoomId(), reservationCode, initialStatus);

        // 7. 保存参会人
        if (dto.getAttendeeUserIds() != null && !dto.getAttendeeUserIds().isEmpty()) {
            attendeeService.inviteAttendees(reservation.getId(), userId, dto.getAttendeeUserIds());
        }
        return reservationCode;
    }

    /**
     * 生成预约编号: B + yyyyMMdd + 6位序列
     * 序列使用自增主键，避免并发冲突与外部依赖（如 Redis）
     */
    private String generateReservationCode(Long id) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "B" + datePart + String.format("%06d", id);
    }

    private void validateRoomRules(MeetingRoom room, ReservationCreateDTO dto) {
        LocalDateTime start = dto.getStartTime();
        LocalDateTime end = dto.getEndTime();
        LocalDateTime now = LocalDateTime.now();

        // 3.1 校验提前预约天数
        if (room.getAdvanceDays() != null && room.getAdvanceDays() > 0) {
            LocalDate bookingDate = start.toLocalDate();
            LocalDate maxDate = now.toLocalDate().plusDays(room.getAdvanceDays());
            if (bookingDate.isAfter(maxDate)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                        "最多提前" + room.getAdvanceDays() + "天预约");
            }
        }

        // 3.2 校验可预约时段
        if (room.getBookableStart() != null && room.getBookableEnd() != null) {
            LocalTime bookableStart = LocalTime.parse(room.getBookableStart());
            LocalTime bookableEnd = LocalTime.parse(room.getBookableEnd());
            LocalTime reservationStart = start.toLocalTime();
            LocalTime reservationEnd = end.toLocalTime();

            if (reservationStart.isBefore(bookableStart) || reservationEnd.isAfter(bookableEnd)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                        "预约时段须在 " + room.getBookableStart() + " ~ " + room.getBookableEnd() + " 之间");
            }
        }

        // 3.3 校验最大预约时长
        if (room.getMaxDuration() != null && room.getMaxDuration() > 0) {
            long durationMinutes = ChronoUnit.MINUTES.between(start, end);
            if (durationMinutes > room.getMaxDuration()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                        "单次预约最长 " + room.getMaxDuration() + " 分钟");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReservation(Long userId, Long reservationId) {
        MeetingRoomReservation reservation = getById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "无权取消他人的预约");
        }
        Integer status = reservation.getStatus();
        if (status.equals(ReservationStatusEnum.CANCELLED.getCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "预约已取消，请勿重复操作");
        }
        if (status.equals(ReservationStatusEnum.REJECTED.getCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "预约已被拒绝，无法取消");
        }
        // 只能取消未进行的预约（开始时间在当前时间之后）
        if (reservation.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "已开始或已结束的预约无法取消");
        }
        reservation.setStatus(ReservationStatusEnum.CANCELLED.getCode());
        updateById(reservation);
        log.info("预约取消, userId={}, reservationId={}", userId, reservationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReservation(Long userId, Long reservationId) {
        MeetingRoomReservation reservation = getById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "无权删除他人的预约");
        }
        if (!isDeletableStatus(reservation.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "只能删除已取消或已拒绝的预约");
        }
        removeById(reservationId);
        log.info("预约删除, userId={}, reservationId={}", userId, reservationId);
    }

    @Override
    public IPage<ReservationVO> listMyReservations(Long userId, ReservationPageQuery query) {
        // 复杂多条件 + JOIN 会议室名称，下沉到 ReservationRepository.xml
        Page<ReservationVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<ReservationVO> result = reservationRepository.selectMyPage(page, query, userId);
        fillUsernames(result.getRecords());
        return result;
    }

    @Override
    public List<ReservationVO> listByRoomAndDate(Long roomId, String date) {
        LocalDate targetDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        List<MeetingRoomReservation> reservations = list(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getRoomId, roomId)
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                        .between(MeetingRoomReservation::getStartTime, dayStart, dayEnd)
                        .orderByAsc(MeetingRoomReservation::getStartTime)
        );
        List<ReservationVO> vos = reservations.stream().map(r -> toVO(r, null, null)).collect(Collectors.toList());
        // 批量回填预约人用户名，供前端已约时段展示
        fillUsernames(vos);
        return vos;
    }

    @Override
    public IPage<ReservationVO> listAllReservations(ReservationPageQuery query) {
        // 预约人 username 为跨服务数据，先通过 Feign 查 userId 再过滤
        if (query.getUsername() != null && !query.getUsername().isEmpty() && query.getUserId() == null) {
            try {
                var userResult = userFeignClient.getUserForAuth(query.getUsername());
                if (userResult != null && userResult.getData() != null) {
                    query.setUserId(userResult.getData().getId());
                } else {
                    // 用户不存在，返回空结果
                    Page<ReservationVO> emptyPage = new Page<>(query.getPage(), query.getSize());
                    emptyPage.setRecords(new ArrayList<>());
                    emptyPage.setTotal(0);
                    return emptyPage;
                }
            } catch (Exception e) {
                log.warn("Feign 查询用户失败, username={}, 降级为不按用户名过滤", query.getUsername(), e);
            }
        }
        // 复杂多条件 + JOIN 会议室名称，下沉到 ReservationRepository.xml
        Page<ReservationVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<ReservationVO> result = reservationRepository.selectAllPage(page, query);
        fillUsernames(result.getRecords());
        return result;
    }

    /**
     * 批量回填预约人用户名：一次 Feign 调用替代逐个拉取，消除 N+1
     * 同时修复原实现将 userId 误作 username 传入 getUserForAuth 的问题
     */
    private void fillUsernames(List<ReservationVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream()
                .map(ReservationVO::getUserId)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> userNameMap = new java.util.HashMap<>();
        try {
            var result = userFeignClient.batchUsernames(userIds);
            if (result != null && result.getData() != null) {
                userNameMap = result.getData();
            }
        } catch (Exception e) {
            log.warn("Feign 批量查询用户名失败, userIds={}, 降级为用户名留空", userIds, e);
        }
        for (ReservationVO vo : records) {
            vo.setUsername(userNameMap.getOrDefault(vo.getUserId(), ""));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReservation(Long reservationId) {
        MeetingRoomReservation reservation = getById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        if (!reservation.getStatus().equals(ReservationStatusEnum.PENDING.getCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "非待确认状态，无法审批通过");
        }
        // CAS 更新：WHERE status=PENDING 避免并发覆盖
        MeetingRoomReservation update = new MeetingRoomReservation();
        update.setId(reservationId);
        update.setStatus(ReservationStatusEnum.CONFIRMED.getCode());
        boolean ok = update(update, new LambdaQueryWrapper<MeetingRoomReservation>()
                .eq(MeetingRoomReservation::getId, reservationId)
                .eq(MeetingRoomReservation::getStatus, ReservationStatusEnum.PENDING.getCode()));
        if (!ok) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "预约已被处理，请刷新后重试");
        }
        log.info("预约审批通过, reservationId={}", reservationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReservation(Long reservationId, String reason) {
        MeetingRoomReservation reservation = getById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        if (!reservation.getStatus().equals(ReservationStatusEnum.PENDING.getCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "非待确认状态，无法拒绝");
        }
        // CAS 更新：WHERE status=PENDING 避免并发覆盖
        MeetingRoomReservation update = new MeetingRoomReservation();
        update.setId(reservationId);
        update.setStatus(ReservationStatusEnum.REJECTED.getCode());
        update.setRejectReason(reason != null && !reason.isBlank() ? reason : "管理员拒绝");
        boolean ok = update(update, new LambdaQueryWrapper<MeetingRoomReservation>()
                .eq(MeetingRoomReservation::getId, reservationId)
                .eq(MeetingRoomReservation::getStatus, ReservationStatusEnum.PENDING.getCode()));
        if (!ok) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "预约已被处理，请刷新后重试");
        }
        log.info("预约拒绝, reservationId={}, reason={}", reservationId, reason);
    }

    /**
     * 可删除状态：已取消 / 已拒绝
     */
    private boolean isDeletableStatus(Integer status) {
        return status != null && (status.equals(ReservationStatusEnum.CANCELLED.getCode())
                || status.equals(ReservationStatusEnum.REJECTED.getCode()));
    }

    private boolean checkTimeConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId) {
        // 区间重叠判定标准公式：existing.start < new.end AND existing.end > new.start
        // 严格区分"相邻"与"重叠"：existing.end == new.start 视为相邻不冲突，
        // 且不依赖 DATETIME 小数秒精度（原 minusNanos/plusNanos 会被 MySQL 截断导致边界误判）
        LambdaQueryWrapper<MeetingRoomReservation> wrapper = new LambdaQueryWrapper<MeetingRoomReservation>()
                .eq(MeetingRoomReservation::getRoomId, roomId)
                .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                .lt(MeetingRoomReservation::getStartTime, endTime)
                .gt(MeetingRoomReservation::getEndTime, startTime);
        if (excludeId != null) {
            wrapper.ne(MeetingRoomReservation::getId, excludeId);
        }
        return count(wrapper) > 0;
    }


    @Override
    public ScheduleVO getSchedule(String date, String startDate, String endDate) {
        LocalDate start, end;
        if (date != null && !date.isEmpty()) {
            start = LocalDate.parse(date);
            end = start;
        } else if (startDate != null && endDate != null) {
            start = LocalDate.parse(startDate);
            end = LocalDate.parse(endDate);
        } else {
            start = LocalDate.now();
            end = start;
        }

        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.atTime(LocalTime.MAX);

        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode()).orderByAsc(MeetingRoom::getName)
        );

        List<MeetingRoomReservation> reservations = list(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                        .lt(MeetingRoomReservation::getStartTime, rangeEnd)
                        .gt(MeetingRoomReservation::getEndTime, rangeStart)
        );

        Map<Long, String> roomNameMap = rooms.stream()
                .collect(Collectors.toMap(MeetingRoom::getId, MeetingRoom::getName));

        // 查询预约人姓名（批量 Feign 调用，消除 N+1）
        List<Long> userIds = reservations.stream()
                .map(MeetingRoomReservation::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> userNameMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            try {
                var result = userFeignClient.batchUsernames(userIds);
                if (result != null && result.getData() != null) {
                    userNameMap = result.getData();
                }
            } catch (Exception e) {
                log.warn("Feign 批量查询用户名失败(getSchedule), userIds={}, 降级为用户名留空", userIds, e);
            }
        }

        ScheduleVO vo = new ScheduleVO();
        List<ScheduleRoomVO> roomVOs = new ArrayList<>();
        for (MeetingRoom r : rooms) {
            ScheduleRoomVO rvo = new ScheduleRoomVO();
            rvo.setId(r.getId());
            rvo.setName(r.getName());
            rvo.setCapacity(r.getCapacity());
            roomVOs.add(rvo);
        }
        vo.setRooms(roomVOs);

        List<ScheduleReservationVO> rsvos = new ArrayList<>();
        for (MeetingRoomReservation r : reservations) {
            ScheduleReservationVO rvo = new ScheduleReservationVO();
            rvo.setId(r.getId());
            rvo.setRoomId(r.getRoomId());
            rvo.setRoomName(roomNameMap.getOrDefault(r.getRoomId(), ""));
            rvo.setSubject(r.getSubject());
            rvo.setUserName(userNameMap.getOrDefault(r.getUserId(), ""));
            rvo.setAttendeeCount(r.getAttendeeCount());
            rvo.setStartTime(r.getStartTime());
            rvo.setEndTime(r.getEndTime());
            rvo.setStatus(r.getStatus());
            rsvos.add(rvo);
        }
        vo.setReservations(rsvos);
        return vo;
    }

    private ReservationVO toVO(MeetingRoomReservation r, Map<Long, String> roomNameMap, Map<Long, String> userNameMap) {
        ReservationVO vo = new ReservationVO();
        vo.setId(r.getId());
        vo.setReservationCode(r.getReservationCode());
        vo.setRoomId(r.getRoomId());
        vo.setRoomName(roomNameMap != null ? roomNameMap.getOrDefault(r.getRoomId(), "") : "");
        vo.setUserId(r.getUserId());
        vo.setUsername(userNameMap != null ? userNameMap.getOrDefault(r.getUserId(), "") : "");
        vo.setSubject(r.getSubject());
        vo.setAttendeeCount(r.getAttendeeCount());
        vo.setRemark(r.getRemark());
        vo.setStartTime(r.getStartTime());
        vo.setEndTime(r.getEndTime());
        vo.setStatus(r.getStatus());
        vo.setRejectReason(r.getRejectReason());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }

    @Override
    public ReservationVO getReservationDetail(Long reservationId) {
        MeetingRoomReservation r = getById(reservationId);
        if (r == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        // 查询会议室名称
        String roomName = "";
        try {
            MeetingRoom room = meetingRoomRepository.selectById(r.getRoomId());
            if (room != null) {
                roomName = room.getName();
            }
        } catch (Exception e) {
            log.warn("查询会议室名称失败, roomId={}", r.getRoomId(), e);
        }

        // 查询用户名（使用批量内部接口，与列表查询保持一致）
        String userName = "";
        try {
            var userResult = userFeignClient.batchUsernames(List.of(r.getUserId()));
            if (userResult != null && userResult.getData() != null) {
                userName = userResult.getData().getOrDefault(r.getUserId(), "");
            }
        } catch (Exception e) {
            log.warn("Feign 查询用户名失败, userId={}", r.getUserId(), e);
        }

        ReservationVO vo = toVO(r, Map.of(r.getRoomId(), roomName), Map.of(r.getUserId(), userName));
        vo.setAttendees(attendeeService.listAttendees(reservationId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteReservation(Long reservationId) {
        MeetingRoomReservation reservation = getById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        if (!isDeletableStatus(reservation.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "只能删除已取消或已拒绝的预约");
        }
        removeById(reservationId);
        log.info("管理员删除预约, reservationId={}", reservationId);
    }
}
