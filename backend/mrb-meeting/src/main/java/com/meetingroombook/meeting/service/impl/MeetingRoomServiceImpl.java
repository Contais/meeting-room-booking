package com.meetingroombook.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetingroombook.common.exception.BusinessException;
import com.meetingroombook.common.exception.ErrorCode;
import com.meetingroombook.meeting.model.entity.MeetingRoom;
import com.meetingroombook.meeting.repository.MeetingRoomRepository;
import com.meetingroombook.meeting.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 会议室服务实现
 */
@Service
@RequiredArgsConstructor
public class MeetingRoomServiceImpl implements MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;

    @Override
    public MeetingRoom getRoomById(Long id) {
        MeetingRoom room = meetingRoomRepository.selectById(id);
        if (room == null) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }
        return room;
    }

    @Override
    public List<MeetingRoom> listRooms() {
        return meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getStatus, 1)
                        .orderByDesc(MeetingRoom::getCreateTime)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRoom(MeetingRoom meetingRoom) {
        meetingRoom.setStatus(1);
        meetingRoomRepository.insert(meetingRoom);
    }
}
