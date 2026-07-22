package com.meetinghub.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.meeting.model.dto.ReservationCreateDTO;
import com.meetinghub.meeting.model.dto.ReservationPageQuery;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.vo.ReservationVO;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final MeetingRoomRepository meetingRoomRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReservation(Long userId, ReservationCreateDTO dto) {
        // 1. 校验会议室存在
        MeetingRoom room = meetingRoomRepository.selectById(dto.getRoomId());
        if (room == null) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }
        if (room.getStatus() == 0) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_DISABLED);
        }

        // 2. 校验时间合法性
        if (dto.getEndTime().isBefore(dto.getStartTime()) || dto.getEndTime().isEqual(dto.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "结束时间必须晚于开始时间");
        }

        // 3. 冲突检测：查询该会议室在该时段是否有已确认的预约
        boolean hasConflict = checkTimeConflict(dto.getRoomId(), dto.getStartTime(), dto.getEndTime(), null);
        if (hasConflict) {
            throw new BusinessException(ErrorCode.RESERVATION_CONFLICT);
        }

        // 4. 创建预约
        MeetingRoomReservation reservation = new MeetingRoomReservation();
        reservation.setRoomId(dto.getRoomId());
        reservation.setUserId(userId);
        reservation.setSubject(dto.getSubject());
        reservation.setAttendeeCount(dto.getAttendeeCount());
        reservation.setContactPhone(dto.getContactPhone());
        reservation.setRemark(dto.getRemark());
        reservation.setStartTime(dto.getStartTime());
        reservation.setEndTime(dto.getEndTime());
        reservation.setStatus(1); // 直接确认（简化流程，不做审批）
        reservationRepository.insert(reservation);
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
        if (reservation.getStatus() == 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "预约已取消");
        }
        reservation.setStatus(2);
        reservationRepository.updateById(reservation);
    }

    @Override
    public IPage<ReservationVO> listMyReservations(Long userId, ReservationPageQuery query) {
        Page<MeetingRoomReservation> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<MeetingRoomReservation> wrapper = new LambdaQueryWrapper<MeetingRoomReservation>()
                .eq(MeetingRoomReservation::getUserId, userId)
                .orderByDesc(MeetingRoomReservation::getStartTime);
        if (query.getStatus() != null) {
            wrapper.eq(MeetingRoomReservation::getStatus, query.getStatus());
        }
        IPage<MeetingRoomReservation> result = reservationRepository.selectPage(page, wrapper);
        return result.convert(r -> toVO(r, null));
    }

    @Override
    public List<ReservationVO> listByRoomAndDate(Long roomId, String date) {
        LocalDate targetDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getRoomId, roomId)
                        .ne(MeetingRoomReservation::getStatus, 2)
                        .between(MeetingRoomReservation::getStartTime, dayStart, dayEnd)
                        .orderByAsc(MeetingRoomReservation::getStartTime)
        );
        return reservations.stream().map(r -> toVO(r, null)).collect(Collectors.toList());
    }

    @Override
    public IPage<ReservationVO> listAllReservations(ReservationPageQuery query) {
        Page<MeetingRoomReservation> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<MeetingRoomReservation> wrapper = new LambdaQueryWrapper<>();
        if (query.getRoomId() != null) {
            wrapper.eq(MeetingRoomReservation::getRoomId, query.getRoomId());
        }
        if (query.getUserId() != null) {
            wrapper.eq(MeetingRoomReservation::getUserId, query.getUserId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(MeetingRoomReservation::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(MeetingRoomReservation::getStartTime);

        IPage<MeetingRoomReservation> result = reservationRepository.selectPage(page, wrapper);

        // 批量查询会议室名称
        List<Long> roomIds = result.getRecords().stream()
                .map(MeetingRoomReservation::getRoomId).distinct().collect(Collectors.toList());
        Map<Long, String> roomNameMap = Map.of();
        if (!roomIds.isEmpty()) {
            List<MeetingRoom> rooms = meetingRoomRepository.selectBatchIds(roomIds);
            roomNameMap = rooms.stream().collect(Collectors.toMap(MeetingRoom::getId, MeetingRoom::getName));
        }
        Map<Long, String> finalRoomNameMap = roomNameMap;
        return result.convert(r -> toVO(r, finalRoomNameMap));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReservation(Long reservationId) {
        MeetingRoomReservation reservation = reservationRepository.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        reservation.setStatus(1);
        reservationRepository.updateById(reservation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReservation(Long reservationId) {
        MeetingRoomReservation reservation = reservationRepository.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        reservation.setStatus(2);
        reservationRepository.updateById(reservation);
    }

    private boolean checkTimeConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId) {
        LambdaQueryWrapper<MeetingRoomReservation> wrapper = new LambdaQueryWrapper<MeetingRoomReservation>()
                .eq(MeetingRoomReservation::getRoomId, roomId)
                .ne(MeetingRoomReservation::getStatus, 2)
                .and(w -> w
                        .between(MeetingRoomReservation::getStartTime, startTime, endTime.minusNanos(1))
                        .or().between(MeetingRoomReservation::getEndTime, startTime.plusNanos(1), endTime)
                        .or().le(MeetingRoomReservation::getStartTime, startTime)
                        .and(inner -> inner.ge(MeetingRoomReservation::getEndTime, endTime))
                );
        if (excludeId != null) {
            wrapper.ne(MeetingRoomReservation::getId, excludeId);
        }
        return reservationRepository.selectCount(wrapper) > 0;
    }

    private ReservationVO toVO(MeetingRoomReservation r, Map<Long, String> roomNameMap) {
        ReservationVO vo = new ReservationVO();
        vo.setId(r.getId());
        vo.setRoomId(r.getRoomId());
        vo.setRoomName(roomNameMap != null ? roomNameMap.getOrDefault(r.getRoomId(), "") : "");
        vo.setUserId(r.getUserId());
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
