package com.meetinghub.meeting.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.io.Serializable;

@Data
public class RoomCreateDTO implements Serializable {
    @NotBlank(message = "会议室名称不能为空")
    private String name;
    private String location;
    @NotNull(message = "容纳人数不能为空")
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
