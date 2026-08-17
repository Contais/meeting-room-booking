package com.meetinghub.meeting.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会议室预约实体
 */
@Data
@TableName("meeting_room_reservation")
public class MeetingRoomReservation extends BaseEntity {

    /**
     * 预约编号: B + yyyyMMdd + 6位序列（Redis 按天自增）
     */
    private String reservationCode;

    /**
     * 会议室ID
     */
    private Long roomId;

    /**
     * 会议室名称快照（创建预约时冗余保存，避免会议室后续删除/改名影响历史展示）
     */
    private String roomName;

    /**
     * 预约用户ID
     */
    private Long userId;

    /**
     * 会议主题
     */
    private String subject;

    /**
     * 参会人数（由参会人列表 count 自动派生）
     */
    private Integer attendeeCount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 预约开始时间
     */
    private LocalDateTime startTime;

    /**
     * 预约结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态: 0-待确认, 1-已确认, 2-已取消, 3-已拒绝
     */
    private Integer status;

    /**
     * 拒绝原因（status=3 时填充）
     */
    private String rejectReason;

}
