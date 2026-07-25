package com.meetinghub.meeting.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
/** 预约视图对象 */
public class ReservationVO implements Serializable {
    private Long id;
    private Long roomId;
    private String roomName;
    private Long userId;
    private String username;
    private String subject;
    private Integer attendeeCount;
    private String contactPhone;
    private String remark;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
}
