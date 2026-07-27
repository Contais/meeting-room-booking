package com.meetinghub.meeting.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增设备请求 DTO
 */
@Data
public class EquipmentCreateDTO implements Serializable {
    @NotBlank(message = "设备编码不能为空")
    private String code;

    @NotBlank(message = "设备名称不能为空")
    private String name;

    private String category;
    private String brand;
    private String model;
    private Integer status;
    private LocalDate purchaseDate;
    private String description;

    /** 创建时一次性关联的会议室ID列表 */
    private List<RoomEquipmentItem> rooms;

    /**
     * 会议室关联项
     */
    @Data
    public static class RoomEquipmentItem implements Serializable {
        private Long roomId;
        private Integer quantity;
    }
}
