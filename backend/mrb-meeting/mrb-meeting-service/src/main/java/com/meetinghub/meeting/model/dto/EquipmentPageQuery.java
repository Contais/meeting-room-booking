package com.meetinghub.meeting.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 设备分页查询参数
 */
@Data
public class EquipmentPageQuery implements Serializable {
    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
    private String name;
    private String category;
    private String brand;
    private Integer status;
    private Long roomId;
    private String createTimeStart;
    private String createTimeEnd;
}
