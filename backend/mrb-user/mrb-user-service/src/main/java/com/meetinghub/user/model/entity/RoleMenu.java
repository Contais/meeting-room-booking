package com.meetinghub.user.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 角色菜单关联实体
 */
@Data
@TableName("role_menu")
public class RoleMenu extends BaseEntity {

    private String role;

    private Long menuId;
}
