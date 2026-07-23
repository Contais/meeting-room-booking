package com.meetinghub.meeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("meeting_room")
public class MeetingRoom {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String location;
    private Integer capacity;
    private String equipment;
    private String imageUrl;
    private String description;
    private Integer status;

    // 使用规则
    private String bookableStart;   // 可预约开始时间 HH:mm
    private String bookableEnd;     // 可预约结束时间 HH:mm
    private Integer maxDuration;    // 最大预约时长（分钟）
    private Integer advanceDays;    // 提前预约天数
    private Integer needApproval;   // 是否需要审批 0-否 1-是

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
