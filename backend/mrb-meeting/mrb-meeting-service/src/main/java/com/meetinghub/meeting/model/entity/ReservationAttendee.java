package com.meetinghub.meeting.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 预约参会人关联实体
 */
@Data
@TableName("reservation_attendee")
public class ReservationAttendee extends BaseEntity {

    /** 预约ID */
    private Long reservationId;

    /** 参会人用户ID */
    private Long userId;

    /** 查阅状态: 0-待查阅, 1-已查阅, 2-已拒绝 */
    private Integer status;

}
