package com.meetinghub.meeting.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 参会人视图对象（用于跨服务回填用户信息后展示）
 */
@Data
public class AttendeeVO implements Serializable {
    /** 用户ID */
    private Long userId;
    /** 用户名 */
    private String username;
    /** 真实姓名 */
    private String realName;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 部门ID */
    private Long departmentId;
    /** 部门名称 */
    private String departmentName;
    /** 头像（presigned URL，由 mrb-user 签名后回填） */
    private String avatar;
    /** 查阅状态: 0-待查阅, 1-已查阅, 2-已拒绝 */
    private Integer status;
    /** 邀请时间（attendee 记录创建时间，用于列表排序与展示） */
    private LocalDateTime createTime;
}
