package com.meetinghub.platform.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 系统配置表实体
 * <p>
 * 存储可动态调整的键值对配置（如预签名有效期、上传大小限制等运营参数）。
 * </p>
 */
@Data
@TableName("platform_config")
public class SysConfig extends BaseEntity {

    /** 配置键（唯一，如 file.presigned.expire） */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 描述 */
    private String description;

}
