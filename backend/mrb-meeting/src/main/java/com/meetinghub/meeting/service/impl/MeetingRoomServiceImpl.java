package com.meetinghub.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.meeting.model.dto.RoomCreateDTO;
import com.meetinghub.meeting.model.dto.RoomPageQuery;
import com.meetinghub.meeting.model.dto.RoomUpdateDTO;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.vo.MeetingRoomVO;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingRoomServiceImpl implements MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;

    @Override
    public List<MeetingRoomVO> listActiveRooms() {
        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getStatus, 1)
                        .orderByDesc(MeetingRoom::getCreateTime)
        );
        return rooms.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public MeetingRoomVO getRoomDetail(Long id) {
        MeetingRoom room = meetingRoomRepository.selectById(id);
        if (room == null) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }
        return toVO(room);
    }

    @Override
    public IPage<MeetingRoomVO> listRooms(RoomPageQuery query) {
        Page<MeetingRoom> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<MeetingRoom> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(MeetingRoom::getName, query.getKeyword())
                    .or().like(MeetingRoom::getLocation, query.getKeyword()));
        }
        if (query.getStatus() != null) {
            wrapper.eq(MeetingRoom::getStatus, query.getStatus());
        }
        if (query.getMinCapacity() != null) {
            wrapper.ge(MeetingRoom::getCapacity, query.getMinCapacity());
        }
        wrapper.orderByDesc(MeetingRoom::getCreateTime);

        return meetingRoomRepository.selectPage(page, wrapper).convert(this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRoom(RoomCreateDTO dto) {
        MeetingRoom room = new MeetingRoom();
        room.setName(dto.getName());
        room.setLocation(dto.getLocation());
        room.setCapacity(dto.getCapacity());
        room.setEquipment(dto.getEquipment());
        room.setImageUrl(dto.getImageUrl());
        room.setDescription(dto.getDescription());
        room.setStatus(1);
        meetingRoomRepository.insert(room);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoom(RoomUpdateDTO dto) {
        MeetingRoom room = meetingRoomRepository.selectById(dto.getId());
        if (room == null) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }
        if (StringUtils.hasText(dto.getName())) room.setName(dto.getName());
        if (dto.getLocation() != null) room.setLocation(dto.getLocation());
        if (dto.getCapacity() != null) room.setCapacity(dto.getCapacity());
        if (dto.getEquipment() != null) room.setEquipment(dto.getEquipment());
        if (dto.getImageUrl() != null) room.setImageUrl(dto.getImageUrl());
        if (dto.getDescription() != null) room.setDescription(dto.getDescription());
        meetingRoomRepository.updateById(room);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        MeetingRoom room = meetingRoomRepository.selectById(id);
        if (room == null) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }
        room.setStatus(room.getStatus() == 1 ? 0 : 1);
        meetingRoomRepository.updateById(room);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoom(Long id) {
        MeetingRoom room = meetingRoomRepository.selectById(id);
        if (room == null) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }
        meetingRoomRepository.deleteById(id);
    }

    private MeetingRoomVO toVO(MeetingRoom room) {
        MeetingRoomVO vo = new MeetingRoomVO();
        vo.setId(room.getId());
        vo.setName(room.getName());
        vo.setLocation(room.getLocation());
        vo.setCapacity(room.getCapacity());
        vo.setEquipment(room.getEquipment());
        vo.setImageUrl(room.getImageUrl());
        vo.setDescription(room.getDescription());
        vo.setStatus(room.getStatus());
        vo.setCreateTime(room.getCreateTime());
        return vo;
    }
}
