package com.meetinghub.meeting.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;
import java.time.LocalDate;

/**
 * 设备实体
 */
@Data
@TableName("meeting_equipment")
public class Equipment extends BaseEntity {

    /** 设备编码 */
    private String code;

    /** 设备名称 */
    private String name;

    /** 设备分类: 投影仪/白板/电视/音响/视频会议/空调/其他 */
    private String category;

    /** 品牌 */
    private String brand;

    /** 型号 */
    private String model;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 购置日期 */
    private LocalDate purchaseDate;

    /** 设备描述 */
    private String description;

}
