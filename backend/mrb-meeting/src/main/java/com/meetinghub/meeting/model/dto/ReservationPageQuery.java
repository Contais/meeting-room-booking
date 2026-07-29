package com.meetinghub.meeting.model.dto;

import lombok.Data;
import java.io.Serializable;

@Data
/**
 * 预约分页查询参数
 */
public class ReservationPageQuery implements Serializable {
    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
    private String subject;
    private Long roomId;
    private Long userId;
    private Integer status;
    private String username;
    private String startTime;
    private String endTime;
    private String reservationCode;
    private String createTimeStart;
    private String createTimeEnd;
    /** 查阅状态过滤（仅我的会议生效）: 0-待查阅, 1-已查阅, 2-已拒绝 */
    private Integer attendeeStatus;
    /** 仅查询即将到来的会议（仅我的会议生效）: start_time > NOW() */
    private Boolean upcoming;
}
