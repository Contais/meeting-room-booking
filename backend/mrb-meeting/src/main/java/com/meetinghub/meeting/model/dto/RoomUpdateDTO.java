package com.meetinghub.meeting.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.io.Serializable;

@Data
/** 编辑会议室请求 DTO */
public class RoomUpdateDTO implements Serializable {
    @NotNull(message = "会议室ID不能为空")
    private Long id;
    private String name;
    private String location;
    @Positive(message = "容纳人数必须大于0")
    private Integer capacity;
    private String equipment;
    private String imageUrl;
    private String description;
    private String bookableStart;
    private String bookableEnd;
    private Integer maxDuration;
    private Integer advanceDays;
    private Integer needApproval;
}
