package com.meetinghub.user.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 用户实体
 */
@Data
@TableName("user")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（BCrypt哈希）
     */
    private String password;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 所属部门ID
     */
    private Long departmentId;

    /**
     * 角色: admin-管理员, user-普通用户
     */
    private String role;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

}
