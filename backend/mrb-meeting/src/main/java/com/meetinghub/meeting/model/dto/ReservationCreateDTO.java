package com.meetinghub.meeting.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
/** 创建预约请求 DTO */
public class ReservationCreateDTO implements Serializable {

    @NotNull(message = "会议室ID不能为空")
    private Long roomId;

    @NotBlank(message = "会议主题不能为空")
    private String subject;

    private Integer attendeeCount;

    private String contactPhone;

    private String remark;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
