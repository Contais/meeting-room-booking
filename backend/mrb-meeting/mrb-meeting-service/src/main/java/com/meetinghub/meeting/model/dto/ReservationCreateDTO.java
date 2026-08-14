package com.meetinghub.meeting.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
/**
 * 创建预约请求 DTO
 */
public class ReservationCreateDTO implements Serializable {

    @NotNull(message = "会议室ID不能为空")
    @Schema(description = "会议室ID")
    private Long roomId;

    @NotBlank(message = "会议主题不能为空")
    @Schema(description = "会议主题")
    private String subject;

    /** 参会人用户ID列表 */
    @Schema(description = "参会人用户ID列表")
    private List<Long> attendeeUserIds;

    @Schema(description = "备注")
    private String remark;

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "开始时间（yyyy-MM-dd HH:mm:ss）")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Schema(description = "结束时间（yyyy-MM-dd HH:mm:ss）")
    private LocalDateTime endTime;
}
