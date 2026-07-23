package com.meetinghub.meeting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.meeting.model.dto.RoomCreateDTO;
import com.meetinghub.meeting.model.dto.RoomPageQuery;
import com.meetinghub.meeting.model.dto.RoomUpdateDTO;
import com.meetinghub.meeting.model.vo.MeetingRoomVO;

import java.util.List;

/**
 * 会议室服务接口
 */
public interface MeetingRoomService {

    /** 查询所有启用的会议室（用户端列表） */
    List<MeetingRoomVO> listActiveRooms();

    /** 查询会议室详情 */
    MeetingRoomVO getRoomDetail(Long id);

    /** 分页查询会议室列表（管理端，支持搜索/筛选） */
    IPage<MeetingRoomVO> listRooms(RoomPageQuery query);

    /** 新增会议室 */
    void createRoom(RoomCreateDTO dto);

    /** 编辑会议室 */
    void updateRoom(RoomUpdateDTO dto);

    /** 启用/禁用会议室 */
    void toggleStatus(Long id);

    /** 删除会议室（逻辑删除） */
    void deleteRoom(Long id);
}
