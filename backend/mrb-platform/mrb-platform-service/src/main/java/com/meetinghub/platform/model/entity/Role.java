package com.meetinghub.platform.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 角色实体
 */
@Data
@TableName("platform_role")
public class Role extends BaseEntity {

    private String roleCode;

    private String roleName;

    private String description;

    private Integer status;

    private Integer isSystem;

    private Integer sort;

}
