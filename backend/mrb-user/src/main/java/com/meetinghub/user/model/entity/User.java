package com.meetinghub.user.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("user")
public class User {

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码（BCrypt哈希） */
    private String password;

    /** 手机号 */
    private String phone;

    /** 真实姓名 */
    private String realName;

    /** 角色: admin-管理员, user-普通用户 */
    private String role;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除: 0-否, 1-是 */
    @TableLogic
    private Integer deleted;
}
