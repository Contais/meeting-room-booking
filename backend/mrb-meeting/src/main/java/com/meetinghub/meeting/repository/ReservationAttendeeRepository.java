package com.meetinghub.meeting.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.meeting.model.entity.ReservationAttendee;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约参会人关联数据访问层
 */
@Mapper
public interface ReservationAttendeeRepository extends BaseMapper<ReservationAttendee> {
}
