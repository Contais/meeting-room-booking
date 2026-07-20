package com.meetingroombook.meeting.service;

import com.meetingroombook.meeting.model.entity.MeetingRoom;

import java.util.List;

/**
 * 会议室服务接口
 */
public interface MeetingRoomService {

    MeetingRoom getRoomById(Long id);

    List<MeetingRoom> listRooms();

    void addRoom(MeetingRoom meetingRoom);
}
