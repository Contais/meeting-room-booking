package com.meetinghub.meeting.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
/**
 * 预约视图对象
 */
public class ReservationVO implements Serializable {
    private Long id;
    private String reservationCode;
    private Long roomId;
    private String roomName;
    private Long userId;
    private String username;
    private String subject;
    private Integer attendeeCount;
    private List<AttendeeVO> attendees;
    private String remark;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String rejectReason;
    private LocalDateTime createTime;
    /** 当前用户对该预约的查阅状态（仅「我的会议」列表填充）: 0-待查阅, 1-已查阅, 2-已拒绝 */
    private Integer myAttendeeStatus;
}
