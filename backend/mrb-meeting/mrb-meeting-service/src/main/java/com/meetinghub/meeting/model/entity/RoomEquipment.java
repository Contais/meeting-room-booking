package com.meetinghub.meeting.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 会议室-设备关联实体
 */
@Data
@TableName("meeting_room_equipment")
public class RoomEquipment extends BaseEntity {

    /** 会议室ID */
    private Long roomId;

    /** 设备ID */
    private Long equipmentId;

    /** 数量 */
    private Integer quantity;

}
