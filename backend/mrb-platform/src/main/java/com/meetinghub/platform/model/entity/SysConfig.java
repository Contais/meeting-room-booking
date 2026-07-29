package com.meetinghub.platform.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置表实体
 * <p>
 * 存储可动态调整的键值对配置（如预签名有效期、上传大小限制等运营参数）。
 * </p>
 */
@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键（唯一，如 file.presigned.expire） */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 描述 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
