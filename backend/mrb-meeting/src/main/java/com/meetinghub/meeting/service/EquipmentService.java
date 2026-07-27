package com.meetinghub.meeting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.meetinghub.meeting.model.dto.EquipmentCreateDTO;
import com.meetinghub.meeting.model.dto.EquipmentPageQuery;
import com.meetinghub.meeting.model.dto.EquipmentUpdateDTO;
import com.meetinghub.meeting.model.entity.Equipment;
import com.meetinghub.meeting.model.vo.EquipmentVO;

import java.util.List;

/**
 * 设备服务接口
 */
public interface EquipmentService extends IService<Equipment> {

    /**
     * 分页查询设备列表
     */
    IPage<EquipmentVO> listEquipments(EquipmentPageQuery query);

    /**
     * 查询设备详情（含关联会议室）
     */
    EquipmentVO getEquipmentDetail(Long id);

    /**
     * 查询所有启用的设备（下拉选择用）
     */
    List<EquipmentVO> listActiveEquipments();

    /**
     * 查询会议室关联的设备列表
     */
    List<EquipmentVO> listEquipmentsByRoomId(Long roomId);

    /**
     * 新增设备（可一次性关联会议室）
     */
    void createEquipment(EquipmentCreateDTO dto);

    /**
     * 编辑设备
     */
    void updateEquipment(EquipmentUpdateDTO dto);

    /**
     * 启用/禁用设备
     */
    void toggleStatus(Long id);

    /**
     * 删除设备（同时解除关联）
     */
    void deleteEquipment(Long id);

    /**
     * 为设备关联会议室（覆盖式）
     */
    void assignRooms(Long equipmentId, List<EquipmentCreateDTO.RoomEquipmentItem> rooms);

    /**
     * 为会议室关联设备（覆盖式）
     */
    void assignEquipments(Long roomId, List<Long> equipmentIds);
}
