package com.meetinghub.meeting.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.meeting.model.entity.RoomEquipment;
import com.meetinghub.meeting.model.vo.EquipmentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会议室-设备关联数据访问层
 */
@Mapper
public interface RoomEquipmentRepository extends BaseMapper<RoomEquipment> {

    /**
     * 查询设备关联的会议室列表（含会议室名称、位置、数量）
     */
    List<EquipmentVO.RoomBriefVO> selectRoomListByEquipmentId(@Param("equipmentId") Long equipmentId);

    /**
     * 查询会议室关联的设备列表（含设备信息）
     */
    List<EquipmentVO> selectEquipmentListByRoomId(@Param("roomId") Long roomId);
}
