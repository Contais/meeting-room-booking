package com.meetinghub.meeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会议室预约实体
 */
@Data
@TableName("meeting_room_reservation")
public class MeetingRoomReservation {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会议室ID */
    private Long roomId;

    /** 预约用户ID */
    private Long userId;

    /** 会议主题 */
    private String subject;

    /** 参会人数 */
    private Integer attendeeCount;

    /** 联系人手机号 */
    private String contactPhone;

    /** 备注 */
    private String remark;

    /** 预约开始时间 */
    private LocalDateTime startTime;

    /** 预约结束时间 */
    private LocalDateTime endTime;

    /** 状态: 0-待确认, 1-已确认, 2-已取消 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除: 0-否, 1-是 */
    @TableLogic
    private Integer deleted;
}
