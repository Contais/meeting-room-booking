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
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.util.HashSet;
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

    /** 会议室默认配置常量 */
    private static final String DEFAULT_BOOKABLE_START = "08:00";
    private static final String DEFAULT_BOOKABLE_END = "20:00";
    private static final Integer DEFAULT_MAX_DURATION = 480;
    private static final Integer DEFAULT_ADVANCE_DAYS = 7;

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
        room.setBookableStart(dto.getBookableStart() != null ? dto.getBookableStart() : DEFAULT_BOOKABLE_START);
        room.setBookableEnd(dto.getBookableEnd() != null ? dto.getBookableEnd() : DEFAULT_BOOKABLE_END);
        room.setMaxDuration(dto.getMaxDuration() != null ? dto.getMaxDuration() : DEFAULT_MAX_DURATION);
        room.setAdvanceDays(dto.getAdvanceDays() != null ? dto.getAdvanceDays() : DEFAULT_ADVANCE_DAYS);
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
        // 自动复制 DTO 中非 null 字段到实体，避免 11 个 if 判断
        BeanUtils.copyProperties(dto, room, getNullPropertyNames(dto));
        updateById(room);
    }

    /**
     * 获取对象中值为 null 的属性名数组，供 BeanUtils.copyProperties 忽略
     */
    private String[] getNullPropertyNames(Object source) {
        java.beans.BeanInfo beanInfo;
        try {
            beanInfo = java.beans.Introspector.getBeanInfo(source.getClass());
        } catch (Exception e) {
            return new String[0];
        }
        Set<String> nullNames = new HashSet<>();
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            try {
                if (pd.getReadMethod() != null && pd.getReadMethod().invoke(source) == null) {
                    nullNames.add(pd.getName());
                }
            } catch (Exception ignored) { /* 跳过不可读属性 */ }
        }
        return nullNames.toArray(new String[0]);
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
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
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
