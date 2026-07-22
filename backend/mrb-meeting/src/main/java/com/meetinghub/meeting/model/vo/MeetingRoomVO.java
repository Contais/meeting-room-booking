package com.meetinghub.meeting.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MeetingRoomVO implements Serializable {
    private Long id;
    private String name;
    private String location;
    private Integer capacity;
    private String equipment;
    private String imageUrl;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
}
