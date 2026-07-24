package com.meetinghub.user.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 菜单实体
 */
@Data
@TableName("menu")
public class Menu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String path;

    private String icon;

    private Long parentId;

    private Integer sortOrder;

    private Integer visible;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
