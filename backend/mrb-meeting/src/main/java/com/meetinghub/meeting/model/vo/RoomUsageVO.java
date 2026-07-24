package com.meetinghub.meeting.model.vo;

import lombok.Data;

@Data
public class RoomUsageVO {
    private Long roomId;
    private String roomName;
    private Integer usedMinutes;
    private Integer totalMinutes;
    private Double usageRate;
}
