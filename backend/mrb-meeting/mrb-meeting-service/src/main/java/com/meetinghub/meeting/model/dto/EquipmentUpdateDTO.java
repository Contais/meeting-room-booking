package com.meetinghub.meeting.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 编辑设备请求 DTO
 */
@Data
public class EquipmentUpdateDTO implements Serializable {
    @NotNull(message = "设备ID不能为空")
    private Long id;

    private String name;
    private String category;
    private String brand;
    private String model;
    private Integer status;
    private LocalDate purchaseDate;
    private String description;
}
