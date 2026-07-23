package com.meetinghub.meeting.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议室数据访问层
 */
@Mapper
public interface MeetingRoomRepository extends BaseMapper<MeetingRoom> {
}
