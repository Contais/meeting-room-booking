package com.meetinghub.meeting.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MeetingRoomRepository extends BaseMapper<MeetingRoom> {
}
