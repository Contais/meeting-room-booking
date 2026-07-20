package com.meetingroombook.meeting.service;

import com.meetingroombook.meeting.model.entity.MeetingRoomReservation;

import java.util.List;

/**
 * 预约服务接口
 */
public interface ReservationService {

    MeetingRoomReservation getReservationById(Long id);

    List<MeetingRoomReservation> listByRoomId(Long roomId);

    void createReservation(MeetingRoomReservation reservation);
}
