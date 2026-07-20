package com.tulingshop.meeting.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tulingshop.meeting.model.entity.MeetingRoom;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MeetingRoomRepository extends BaseMapper<MeetingRoom> {
}
