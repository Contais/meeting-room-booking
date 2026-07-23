package com.meetinghub.meeting.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约数据访问层
 */
@Mapper
public interface ReservationRepository extends BaseMapper<MeetingRoomReservation> {
}
