package com.meetingroombook.meeting.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetingroombook.meeting.model.entity.MeetingRoomReservation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReservationRepository extends BaseMapper<MeetingRoomReservation> {
}
