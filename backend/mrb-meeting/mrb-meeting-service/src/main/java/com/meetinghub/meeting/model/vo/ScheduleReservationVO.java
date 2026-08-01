package com.meetinghub.meeting.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ScheduleReservationVO {
    private Long id;
    private Long roomId;
    private String roomName;
    private String subject;
    private String userName;
    private Integer attendeeCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
}
