package com.meetinghub.meeting.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReservationRepository extends BaseMapper<MeetingRoomReservation> {
}
