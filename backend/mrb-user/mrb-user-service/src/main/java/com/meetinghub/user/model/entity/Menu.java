package com.meetinghub.user.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 菜单实体
 */
@Data
@TableName("menu")
public class Menu extends BaseEntity {

    private String name;

    private String path;

    private String icon;

    private Long parentId;

    private Integer sortOrder;

    private Integer visible;

    private Integer status;

}
