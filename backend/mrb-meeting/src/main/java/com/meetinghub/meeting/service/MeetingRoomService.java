package com.meetinghub.meeting.service;

import com.meetinghub.meeting.model.entity.MeetingRoom;

import java.util.List;

/**
 * 会议室服务接口
 */
public interface MeetingRoomService {

    MeetingRoom getRoomById(Long id);

    List<MeetingRoom> listRooms();

    void addRoom(MeetingRoom meetingRoom);
}
