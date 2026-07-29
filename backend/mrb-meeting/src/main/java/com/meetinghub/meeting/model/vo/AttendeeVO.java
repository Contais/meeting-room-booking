package com.meetinghub.meeting.model.vo;

import lombok.Data;
import java.io.Serializable;

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
    /** 查阅状态: 0-待查阅, 1-已查阅, 2-已拒绝 */
    private Integer status;
}
