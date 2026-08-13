package com.meetinghub.platform.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 角色菜单关联实体
 */
@Data
@TableName("platform_role_menu")
public class RoleMenu extends BaseEntity {

    private Long roleId;

    private Long menuId;
}
