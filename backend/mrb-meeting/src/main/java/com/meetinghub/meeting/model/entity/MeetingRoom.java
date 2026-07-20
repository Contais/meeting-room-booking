package com.meetinghub.meeting.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议室实体
 */
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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
