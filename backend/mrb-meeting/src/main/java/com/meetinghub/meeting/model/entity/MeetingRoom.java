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

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会议室名称 */
    private String name;

    /** 位置（楼层/房间号） */
    private String location;

    /** 最大容纳人数 */
    private Integer capacity;

    /** 设备设施（投影仪、白板等） */
    private String equipment;

    /** 实景图片URL */
    private String imageUrl;

    /** 会议室描述 */
    private String description;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 可预约开始时间 HH:mm */
    private String bookableStart;

    /** 可预约结束时间 HH:mm */
    private String bookableEnd;

    /** 单次最大预约时长（分钟） */
    private Integer maxDuration;

    /** 提前预约天数限制 */
    private Integer advanceDays;

    /** 是否需要管理员审批: 0-免审批, 1-需审批 */
    private Integer needApproval;

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
