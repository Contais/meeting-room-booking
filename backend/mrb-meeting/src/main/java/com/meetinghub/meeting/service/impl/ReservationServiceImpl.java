package com.meetinghub.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 预约服务实现
 */
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;

    @Override
    public MeetingRoomReservation getReservationById(Long id) {
        MeetingRoomReservation reservation = reservationRepository.selectById(id);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        return reservation;
    }

    @Override
    public List<MeetingRoomReservation> listByRoomId(Long roomId) {
        return reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getRoomId, roomId)
                        .orderByAsc(MeetingRoomReservation::getStartTime)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReservation(MeetingRoomReservation reservation) {
        // TODO: 校验时段冲突
        reservation.setStatus(0);
        reservationRepository.insert(reservation);
    }
}
