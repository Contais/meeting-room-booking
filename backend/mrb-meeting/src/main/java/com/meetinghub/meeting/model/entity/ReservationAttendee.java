package com.meetinghub.meeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 预约参会人关联实体
 */
@Data
@TableName("reservation_attendee")
public class ReservationAttendee {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约ID */
    private Long reservationId;

    /** 参会人用户ID */
    private Long userId;

    /** 参会状态: 0-待响应, 1-已接受, 2-已拒绝 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
