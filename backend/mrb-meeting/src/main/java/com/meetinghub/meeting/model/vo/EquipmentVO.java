package com.meetinghub.meeting.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备视图对象
 */
@Data
public class EquipmentVO implements Serializable {
    private Long id;
    private String code;
    private String name;
    private String category;
    private String brand;
    private String model;
    private Integer status;
    private LocalDate purchaseDate;
    private String description;
    private LocalDateTime createTime;

    /** 关联数量（用于会议室-设备关联查询时返回） */
    private Integer quantity;

    /** 关联的会议室列表 */
    private List<RoomBriefVO> rooms;

    /**
     * 会议室简要信息
     */
    @Data
    public static class RoomBriefVO implements Serializable {
        private Long id;
        private String name;
        private String location;
        private Integer quantity;
    }
}
