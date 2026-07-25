package com.meetinghub.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.meetinghub.meeting.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final UserFeignClient userFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReservation(Long userId, ReservationCreateDTO dto) {
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
        reservation.setAttendeeCount(dto.getAttendeeCount());
        reservation.setContactPhone(dto.getContactPhone());
        reservation.setRemark(dto.getRemark());
        reservation.setStartTime(dto.getStartTime());
        reservation.setEndTime(dto.getEndTime());
        Integer initialStatus = room.getNeedApproval().equals(ApprovalModeEnum.NEED_APPROVAL.getCode())
                ? ReservationStatusEnum.PENDING.getCode()
                : ReservationStatusEnum.CONFIRMED.getCode();
        reservation.setStatus(initialStatus);
        reservationRepository.insert(reservation);
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
        MeetingRoomReservation reservation = reservationRepository.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "无权取消他人的预约");
        }
        if (reservation.getStatus().equals(ReservationStatusEnum.CANCELLED.getCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "预约已取消");
        }
        reservation.setStatus(ReservationStatusEnum.CANCELLED.getCode());
        reservationRepository.updateById(reservation);
    }

    @Override
    public IPage<ReservationVO> listMyReservations(Long userId, ReservationPageQuery query) {
        Page<MeetingRoomReservation> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<MeetingRoomReservation> wrapper = new LambdaQueryWrapper<MeetingRoomReservation>()
                .eq(MeetingRoomReservation::getUserId, userId)
                .orderByDesc(MeetingRoomReservation::getStartTime);
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(MeetingRoomReservation::getSubject, query.getKeyword());
        }
        if (StringUtils.hasText(query.getSubject())) {
            wrapper.like(MeetingRoomReservation::getSubject, query.getSubject());
        }
        if (query.getStatus() != null) {
            wrapper.eq(MeetingRoomReservation::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getStartTime())) {
            // 搜索时间段包含该开始时间的预约：startTime <= query.startTime AND endTime >= query.startTime
            wrapper.le(MeetingRoomReservation::getStartTime, query.getStartTime());
            wrapper.ge(MeetingRoomReservation::getEndTime, query.getStartTime());
        }
        if (StringUtils.hasText(query.getEndTime())) {
            // 搜索时间段包含该结束时间的预约：startTime <= query.endTime AND endTime >= query.endTime
            wrapper.le(MeetingRoomReservation::getStartTime, query.getEndTime());
            wrapper.ge(MeetingRoomReservation::getEndTime, query.getEndTime());
        }
        IPage<MeetingRoomReservation> result = reservationRepository.selectPage(page, wrapper);

        List<Long> roomIds = result.getRecords().stream()
                .map(MeetingRoomReservation::getRoomId).distinct().collect(Collectors.toList());
        List<Long> userIds = result.getRecords().stream()
                .map(MeetingRoomReservation::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> userNameMap = new java.util.HashMap<>();
        for (Long uid : userIds) {
            try {
                var userResult = userFeignClient.getUserForAuth(String.valueOf(uid));
                if (userResult != null && userResult.getData() != null) {
                    userNameMap.put(uid, userResult.getData().getUsername());
                }
            } catch (Exception e) { /* ignore */ }
        }
        Map<Long, String> roomNameMap = Map.of();
        if (!roomIds.isEmpty()) {
            List<MeetingRoom> rooms = meetingRoomRepository.selectBatchIds(roomIds);
            roomNameMap = rooms.stream().collect(Collectors.toMap(MeetingRoom::getId, MeetingRoom::getName));
        }
        Map<Long, String> finalRoomNameMap = roomNameMap;
        return result.convert(r -> toVO(r, finalRoomNameMap, userNameMap));
    }

    @Override
    public List<ReservationVO> listByRoomAndDate(Long roomId, String date) {
        LocalDate targetDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getRoomId, roomId)
                        .ne(MeetingRoomReservation::getStatus, ReservationStatusEnum.CANCELLED.getCode())
                        .between(MeetingRoomReservation::getStartTime, dayStart, dayEnd)
                        .orderByAsc(MeetingRoomReservation::getStartTime)
        );
        return reservations.stream().map(r -> toVO(r, null, null)).collect(Collectors.toList());
    }

    @Override
    public IPage<ReservationVO> listAllReservations(ReservationPageQuery query) {
        Page<MeetingRoomReservation> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<MeetingRoomReservation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(MeetingRoomReservation::getSubject, query.getKeyword());
        }
        if (query.getRoomId() != null) {
            wrapper.eq(MeetingRoomReservation::getRoomId, query.getRoomId());
        }
        if (query.getUserId() != null) {
            wrapper.eq(MeetingRoomReservation::getUserId, query.getUserId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(MeetingRoomReservation::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getSubject())) {
            wrapper.like(MeetingRoomReservation::getSubject, query.getSubject());
        }
        if (StringUtils.hasText(query.getContactPhone())) {
            wrapper.like(MeetingRoomReservation::getContactPhone, query.getContactPhone());
        }
        if (StringUtils.hasText(query.getStartTime())) {
            wrapper.ge(MeetingRoomReservation::getStartTime, query.getStartTime());
        }
        if (StringUtils.hasText(query.getEndTime())) {
            wrapper.le(MeetingRoomReservation::getEndTime, query.getEndTime());
        }
        wrapper.orderByDesc(MeetingRoomReservation::getStartTime);

        IPage<MeetingRoomReservation> result = reservationRepository.selectPage(page, wrapper);

        List<Long> roomIds = result.getRecords().stream()
                .map(MeetingRoomReservation::getRoomId).distinct().collect(Collectors.toList());
        List<Long> userIds = result.getRecords().stream()
                .map(MeetingRoomReservation::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> userNameMap = new java.util.HashMap<>();
        for (Long uid : userIds) {
            try {
                var userResult = userFeignClient.getUserForAuth(String.valueOf(uid));
                if (userResult != null && userResult.getData() != null) {
                    userNameMap.put(uid, userResult.getData().getUsername());
                }
            } catch (Exception e) { /* ignore */ }
        }
        Map<Long, String> roomNameMap = Map.of();
        if (!roomIds.isEmpty()) {
            List<MeetingRoom> rooms = meetingRoomRepository.selectBatchIds(roomIds);
            roomNameMap = rooms.stream().collect(Collectors.toMap(MeetingRoom::getId, MeetingRoom::getName));
        }
        Map<Long, String> finalRoomNameMap = roomNameMap;
        return result.convert(r -> toVO(r, finalRoomNameMap, userNameMap));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReservation(Long reservationId) {
        MeetingRoomReservation reservation = reservationRepository.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        reservation.setStatus(ReservationStatusEnum.CONFIRMED.getCode());
        reservationRepository.updateById(reservation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReservation(Long reservationId) {
        MeetingRoomReservation reservation = reservationRepository.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        reservation.setStatus(ReservationStatusEnum.CANCELLED.getCode());
        reservationRepository.updateById(reservation);
    }

    private boolean checkTimeConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId) {
        // 区间重叠判定标准公式：existing.start < new.end AND existing.end > new.start
        // 严格区分"相邻"与"重叠"：existing.end == new.start 视为相邻不冲突，
        // 且不依赖 DATETIME 小数秒精度（原 minusNanos/plusNanos 会被 MySQL 截断导致边界误判）
        LambdaQueryWrapper<MeetingRoomReservation> wrapper = new LambdaQueryWrapper<MeetingRoomReservation>()
                .eq(MeetingRoomReservation::getRoomId, roomId)
                .ne(MeetingRoomReservation::getStatus, ReservationStatusEnum.CANCELLED.getCode())
                .lt(MeetingRoomReservation::getStartTime, endTime)
                .gt(MeetingRoomReservation::getEndTime, startTime);
        if (excludeId != null) {
            wrapper.ne(MeetingRoomReservation::getId, excludeId);
        }
        return reservationRepository.selectCount(wrapper) > 0;
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

        List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .ne(MeetingRoomReservation::getStatus, ReservationStatusEnum.CANCELLED.getCode())
                        .lt(MeetingRoomReservation::getStartTime, rangeEnd)
                        .gt(MeetingRoomReservation::getEndTime, rangeStart)
        );

        Map<Long, String> roomNameMap = rooms.stream()
                .collect(Collectors.toMap(MeetingRoom::getId, MeetingRoom::getName));

        // 查询预约人姓名
        List<Long> userIds = reservations.stream()
                .map(MeetingRoomReservation::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> userNameMap = new java.util.HashMap<>();
        for (Long uid : userIds) {
            try {
                var userResult = userFeignClient.getUserForAuth(String.valueOf(uid));
                if (userResult != null && userResult.getData() != null) {
                    userNameMap.put(uid, userResult.getData().getUsername());
                }
            } catch (Exception ignored) {}
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
        vo.setRoomId(r.getRoomId());
        vo.setRoomName(roomNameMap != null ? roomNameMap.getOrDefault(r.getRoomId(), "") : "");
        vo.setUserId(r.getUserId());
        vo.setUsername(userNameMap != null ? userNameMap.getOrDefault(r.getUserId(), "") : "");
        vo.setSubject(r.getSubject());
        vo.setAttendeeCount(r.getAttendeeCount());
        vo.setContactPhone(r.getContactPhone());
        vo.setRemark(r.getRemark());
        vo.setStartTime(r.getStartTime());
        vo.setEndTime(r.getEndTime());
        vo.setStatus(r.getStatus());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}
