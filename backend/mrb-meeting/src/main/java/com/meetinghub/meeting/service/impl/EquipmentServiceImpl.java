package com.meetinghub.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.meeting.model.dto.EquipmentCreateDTO;
import com.meetinghub.meeting.model.dto.EquipmentPageQuery;
import com.meetinghub.meeting.model.dto.EquipmentUpdateDTO;
import com.meetinghub.meeting.model.entity.Equipment;
import com.meetinghub.meeting.model.entity.RoomEquipment;
import com.meetinghub.meeting.model.vo.EquipmentVO;
import com.meetinghub.meeting.repository.EquipmentRepository;
import com.meetinghub.meeting.repository.RoomEquipmentRepository;
import com.meetinghub.meeting.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备服务实现
 */
@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl extends ServiceImpl<EquipmentRepository, Equipment> implements EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final RoomEquipmentRepository roomEquipmentRepository;

    @Override
    public IPage<EquipmentVO> listEquipments(EquipmentPageQuery query) {
        Page<Equipment> page = new Page<>(query.getPage(), query.getSize());
        IPage<EquipmentVO> voPage = equipmentRepository.selectEquipmentPage(page, query).convert(this::toVO);
        // 批量填充关联会议室（数量）
        for (EquipmentVO vo : voPage.getRecords()) {
            vo.setRooms(roomEquipmentRepository.selectRoomListByEquipmentId(vo.getId()));
        }
        return voPage;
    }

    @Override
    public EquipmentVO getEquipmentDetail(Long id) {
        Equipment equipment = getById(id);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }
        EquipmentVO vo = toVO(equipment);
        vo.setRooms(roomEquipmentRepository.selectRoomListByEquipmentId(id));
        return vo;
    }

    @Override
    public List<EquipmentVO> listActiveEquipments() {
        List<Equipment> list = list(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getStatus, EnableStatusEnum.ENABLED.getCode())
                .orderByDesc(Equipment::getCreateTime));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<EquipmentVO> listEquipmentsByRoomId(Long roomId) {
        return roomEquipmentRepository.selectEquipmentListByRoomId(roomId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createEquipment(EquipmentCreateDTO dto) {
        // 校验编码唯一
        long existCount = count(new LambdaQueryWrapper<Equipment>().eq(Equipment::getCode, dto.getCode()));
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.EQUIPMENT_CODE_DUPLICATE);
        }
        Equipment equipment = new Equipment();
        equipment.setCode(dto.getCode());
        equipment.setName(dto.getName());
        equipment.setCategory(dto.getCategory());
        equipment.setBrand(dto.getBrand());
        equipment.setModel(dto.getModel());
        equipment.setStatus(dto.getStatus() != null ? dto.getStatus() : EnableStatusEnum.ENABLED.getCode());
        equipment.setPurchaseDate(dto.getPurchaseDate());
        equipment.setDescription(dto.getDescription());
        save(equipment);
        // 一次性关联会议室
        if (dto.getRooms() != null && !dto.getRooms().isEmpty()) {
            assignRooms(equipment.getId(), dto.getRooms());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipment(EquipmentUpdateDTO dto) {
        Equipment equipment = getById(dto.getId());
        if (equipment == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }
        if (StringUtils.hasText(dto.getName())) equipment.setName(dto.getName());
        if (dto.getCategory() != null) equipment.setCategory(dto.getCategory());
        if (dto.getBrand() != null) equipment.setBrand(dto.getBrand());
        if (dto.getModel() != null) equipment.setModel(dto.getModel());
        if (dto.getStatus() != null) equipment.setStatus(dto.getStatus());
        if (dto.getPurchaseDate() != null) equipment.setPurchaseDate(dto.getPurchaseDate());
        if (dto.getDescription() != null) equipment.setDescription(dto.getDescription());
        updateById(equipment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        Equipment equipment = getById(id);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }
        Integer newStatus = equipment.getStatus().equals(EnableStatusEnum.ENABLED.getCode())
                ? EnableStatusEnum.DISABLED.getCode()
                : EnableStatusEnum.ENABLED.getCode();
        equipment.setStatus(newStatus);
        updateById(equipment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEquipment(Long id) {
        Equipment equipment = getById(id);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }
        removeById(id);
        // 解除关联关系（逻辑删除）
        List<RoomEquipment> relations = roomEquipmentRepository.selectList(
                new LambdaQueryWrapper<RoomEquipment>().eq(RoomEquipment::getEquipmentId, id)
        );
        for (RoomEquipment rel : relations) {
            roomEquipmentRepository.deleteById(rel.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRooms(Long equipmentId, List<EquipmentCreateDTO.RoomEquipmentItem> rooms) {
        if (equipmentId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Equipment equipment = getById(equipmentId);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }
        // 先删除原有关联（逻辑删除）
        List<RoomEquipment> existing = roomEquipmentRepository.selectList(
                new LambdaQueryWrapper<RoomEquipment>().eq(RoomEquipment::getEquipmentId, equipmentId)
        );
        for (RoomEquipment rel : existing) {
            roomEquipmentRepository.deleteById(rel.getId());
        }
        // 再批量插入
        if (rooms != null && !rooms.isEmpty()) {
            for (EquipmentCreateDTO.RoomEquipmentItem item : rooms) {
                RoomEquipment rel = new RoomEquipment();
                rel.setRoomId(item.getRoomId());
                rel.setEquipmentId(equipmentId);
                rel.setQuantity(item.getQuantity() != null ? item.getQuantity() : 1);
                roomEquipmentRepository.insert(rel);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignEquipments(Long roomId, List<Long> equipmentIds) {
        if (roomId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 先删除该会议室原有设备关联
        List<RoomEquipment> existing = roomEquipmentRepository.selectList(
                new LambdaQueryWrapper<RoomEquipment>().eq(RoomEquipment::getRoomId, roomId)
        );
        for (RoomEquipment rel : existing) {
            roomEquipmentRepository.deleteById(rel.getId());
        }
        // 再批量插入
        if (equipmentIds != null && !equipmentIds.isEmpty()) {
            for (Long eqId : equipmentIds) {
                RoomEquipment rel = new RoomEquipment();
                rel.setRoomId(roomId);
                rel.setEquipmentId(eqId);
                rel.setQuantity(1);
                roomEquipmentRepository.insert(rel);
            }
        }
    }

    private EquipmentVO toVO(Equipment equipment) {
        EquipmentVO vo = new EquipmentVO();
        vo.setId(equipment.getId());
        vo.setCode(equipment.getCode());
        vo.setName(equipment.getName());
        vo.setCategory(equipment.getCategory());
        vo.setBrand(equipment.getBrand());
        vo.setModel(equipment.getModel());
        vo.setStatus(equipment.getStatus());
        vo.setPurchaseDate(equipment.getPurchaseDate());
        vo.setDescription(equipment.getDescription());
        vo.setCreateTime(equipment.getCreateTime());
        return vo;
    }
}
