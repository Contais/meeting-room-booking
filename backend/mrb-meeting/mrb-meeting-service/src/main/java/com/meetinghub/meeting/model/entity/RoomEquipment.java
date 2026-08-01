package com.meetinghub.meeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会议室-设备关联实体
 */
@Data
@TableName("room_equipment")
public class RoomEquipment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会议室ID */
    private Long roomId;

    /** 设备ID */
    private Long equipmentId;

    /** 数量 */
    private Integer quantity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
