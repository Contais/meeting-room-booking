package com.meetinghub.platform.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 字典表实体
 * <p>
 * 用于管理枚举型配置数据（如性别、预约状态展示文案等），便于运营动态调整。
 * </p>
 */
@Data
@TableName("platform_dict")
public class SysDict extends BaseEntity {

    /** 字典编码（唯一，如 gender、reservation_status） */
    private String code;

    /** 字典名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

}
