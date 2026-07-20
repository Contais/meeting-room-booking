package com.tulingshop.meeting.service;

import com.tulingshop.meeting.model.entity.MeetingRoomReservation;

import java.util.List;

/**
 * 预约服务接口
 */
public interface ReservationService {

    MeetingRoomReservation getReservationById(Long id);

    List<MeetingRoomReservation> listByRoomId(Long roomId);

    void createReservation(MeetingRoomReservation reservation);
}
