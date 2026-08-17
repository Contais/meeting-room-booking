package com.meetinghub.meeting.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

@Data
/**
 * 新增会议室请求 DTO
 */
public class RoomCreateDTO implements Serializable {
    @NotBlank(message = "会议室名称不能为空")
    @Schema(description = "会议室名称")
    private String name;
    @Schema(description = "位置（楼层/房间号）")
    private String location;
    @NotNull(message = "容纳人数不能为空")
    @Positive(message = "容纳人数必须大于0")
    @Schema(description = "容纳人数")
    private Integer capacity;
    @Schema(description = "设备设施")
    private String equipment;
    @Schema(description = "实景图片 objectKey")
    private String imageUrl;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "可预约开始时间 HH:mm")
    private String bookableStart;
    @Schema(description = "可预约结束时间 HH:mm")
    private String bookableEnd;
    @Schema(description = "单次最小预约时长（分钟），0表示不限制")
    private Integer minDuration;
    @Schema(description = "单次最大预约时长（分钟）")
    private Integer maxDuration;
    @Schema(description = "提前预约天数")
    private Integer advanceDays;
    @Schema(description = "是否需要审批：0-免审批，1-需审批")
    private Integer needApproval;
}
