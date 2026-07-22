package com.meetinghub.meeting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.meeting.model.dto.RoomCreateDTO;
import com.meetinghub.meeting.model.dto.RoomPageQuery;
import com.meetinghub.meeting.model.dto.RoomUpdateDTO;
import com.meetinghub.meeting.model.vo.MeetingRoomVO;

import java.util.List;

public interface MeetingRoomService {

    // 公开接口
    List<MeetingRoomVO> listActiveRooms();

    MeetingRoomVO getRoomDetail(Long id);

    // 管理接口
    IPage<MeetingRoomVO> listRooms(RoomPageQuery query);

    void createRoom(RoomCreateDTO dto);

    void updateRoom(RoomUpdateDTO dto);

    void toggleStatus(Long id);

    void deleteRoom(Long id);
}
