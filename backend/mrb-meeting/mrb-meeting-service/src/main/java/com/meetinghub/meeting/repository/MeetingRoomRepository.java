package com.meetinghub.meeting.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.meeting.model.dto.RoomPageQuery;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 会议室数据访问层
 */
@Mapper
public interface MeetingRoomRepository extends BaseMapper<MeetingRoom> {

    /**
     * 会议室列表分页查询（关键字 OR 检索 + 多条件动态过滤，下沉 XML 提升可读性）
     *
     * @param page  分页参数
     * @param query 过滤条件
     */
    IPage<MeetingRoom> selectRoomPage(IPage<MeetingRoom> page, @Param("query") RoomPageQuery query);
}
