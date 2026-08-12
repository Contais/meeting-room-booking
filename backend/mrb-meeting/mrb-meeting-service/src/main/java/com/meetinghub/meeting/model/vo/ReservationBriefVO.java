package com.meetinghub.meeting.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约简要信息 VO（供 AI 工具返回使用）
 * <p>
 * 仅包含展示给用户的安全字段，屏蔽 userId、roomId、deleted 等内部/敏感字段。
 * </p>
 */
@Data
public class ReservationBriefVO implements Serializable {
    /** 预约编号：B + yyyyMMdd + 6 位序列（Redis 按天自增） */
    private String reservationCode;
    /** 会议主题 */
    private String subject;
    /** 会议室名称（由 roomId 关联查询回填） */
    private String roomName;
    /** 开始时间 */
    private LocalDateTime startTime;
    /** 结束时间 */
    private LocalDateTime endTime;
    /** 状态: 0-待确认, 1-已确认, 2-已取消 */
    private Integer status;
    /** 参会人数 */
    private Integer attendeeCount;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createTime;
}
