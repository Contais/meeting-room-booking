package com.meetinghub.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.enums.ApprovalModeEnum;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.enums.ReservationStatusEnum;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.meeting.model.dto.RoomCreateDTO;
import com.meetinghub.meeting.model.dto.RoomPageQuery;
import com.meetinghub.meeting.model.dto.RoomUpdateDTO;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.vo.MeetingRoomVO;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 会议室服务实现
 */
@Service
@RequiredArgsConstructor
public class MeetingRoomServiceImpl extends ServiceImpl<MeetingRoomRepository, MeetingRoom> implements MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public List<MeetingRoomVO> listActiveRooms() {
        List<MeetingRoom> rooms = list(
                new LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
                        .orderByDesc(MeetingRoom::getCreateTime)
        );
        List<MeetingRoomVO> voList = rooms.stream().map(this::toVO).collect(Collectors.toList());
        fillCurrentAvailable(voList);
        return voList;
    }

    @Override
    public MeetingRoomVO getRoomDetail(Long id) {
        MeetingRoom room = getById(id);
        if (room == null) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }
        MeetingRoomVO vo = toVO(room);
        fillCurrentAvailable(List.of(vo));
        return vo;
    }

    @Override
    public IPage<MeetingRoomVO> listRooms(RoomPageQuery query) {
        Page<MeetingRoom> page = new Page<>(query.getPage(), query.getSize());
        IPage<MeetingRoomVO> voPage = meetingRoomRepository.selectRoomPage(page, query).convert(this::toVO);
        fillCurrentAvailable(voPage.getRecords());
        return voPage;
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
        room.setBookableStart(dto.getBookableStart() != null ? dto.getBookableStart() : "08:00");
        room.setBookableEnd(dto.getBookableEnd() != null ? dto.getBookableEnd() : "20:00");
        room.setMaxDuration(dto.getMaxDuration() != null ? dto.getMaxDuration() : 480);
        room.setAdvanceDays(dto.getAdvanceDays() != null ? dto.getAdvanceDays() : 7);
        room.setNeedApproval(dto.getNeedApproval() != null ? dto.getNeedApproval() : ApprovalModeEnum.FREE_APPROVAL.getCode());
        room.setStatus(EnableStatusEnum.ENABLED.getCode());
        save(room);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoom(RoomUpdateDTO dto) {
        MeetingRoom room = getById(dto.getId());
        if (room == null) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }
        if (StringUtils.hasText(dto.getName())) room.setName(dto.getName());
        if (dto.getLocation() != null) room.setLocation(dto.getLocation());
        if (dto.getCapacity() != null) room.setCapacity(dto.getCapacity());
        if (dto.getEquipment() != null) room.setEquipment(dto.getEquipment());
        if (dto.getImageUrl() != null) room.setImageUrl(dto.getImageUrl());
        if (dto.getDescription() != null) room.setDescription(dto.getDescription());
        if (dto.getBookableStart() != null) room.setBookableStart(dto.getBookableStart());
        if (dto.getBookableEnd() != null) room.setBookableEnd(dto.getBookableEnd());
        if (dto.getMaxDuration() != null) room.setMaxDuration(dto.getMaxDuration());
        if (dto.getAdvanceDays() != null) room.setAdvanceDays(dto.getAdvanceDays());
        if (dto.getNeedApproval() != null) room.setNeedApproval(dto.getNeedApproval());
        updateById(room);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        MeetingRoom room = getById(id);
        if (room == null) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }
        Integer newStatus = room.getStatus().equals(EnableStatusEnum.ENABLED.getCode())
                ? EnableStatusEnum.DISABLED.getCode()
                : EnableStatusEnum.ENABLED.getCode();
        room.setStatus(newStatus);
        updateById(room);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoom(Long id) {
        MeetingRoom room = getById(id);
        if (room == null) {
            throw new BusinessException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }
        removeById(id);
    }

    /**
     * 批量填充会议室的当前空闲状态
     * 查询当前时间段内有未取消预约的会议室ID集合
     */
    private void fillCurrentAvailable(List<MeetingRoomVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        Set<Long> roomIds = voList.stream().map(MeetingRoomVO::getId).collect(Collectors.toSet());
        LocalDateTime now = LocalDateTime.now();
        List<MeetingRoomReservation> activeReservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .in(MeetingRoomReservation::getRoomId, roomIds)
                        .ne(MeetingRoomReservation::getStatus, ReservationStatusEnum.CANCELLED.getCode())
                        .le(MeetingRoomReservation::getStartTime, now)
                        .gt(MeetingRoomReservation::getEndTime, now)
        );
        Set<Long> busyRoomIds = activeReservations.stream()
                .map(MeetingRoomReservation::getRoomId)
                .collect(Collectors.toSet());
        for (MeetingRoomVO vo : voList) {
            // 禁用会议室不算空闲
            vo.setCurrentAvailable(vo.getStatus() != null
                    && vo.getStatus().equals(EnableStatusEnum.ENABLED.getCode())
                    && !busyRoomIds.contains(vo.getId()));
        }
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
        vo.setBookableStart(room.getBookableStart());
        vo.setBookableEnd(room.getBookableEnd());
        vo.setMaxDuration(room.getMaxDuration());
        vo.setAdvanceDays(room.getAdvanceDays());
        vo.setNeedApproval(room.getNeedApproval());
        vo.setCreateTime(room.getCreateTime());
        return vo;
    }
}
