package com.meetinghub.meeting.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.meeting.model.dto.RoomPageQuery;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 按 ID 加行锁查询会议室，用于预约创建时串行化同一会议室的冲突检测。
     *
     * @param id 会议室ID
     * @return 会议室实体，不存在时返回 null
     */
    @Select("SELECT * FROM meeting_room WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    MeetingRoom selectByIdForUpdate(@Param("id") Long id);
}
