package com.meetinghub.platform.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 站内信通知实体
 */
@Data
@TableName("platform_notification")
public class Notification extends BaseEntity {

    /** 接收人ID */
    private Long userId;

    /** 类型: RESERVATION_CREATED/APPROVED/REJECTED/CANCELLED/SYSTEM */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 关联业务类型 (reservation/user) */
    private String refType;

    /** 关联业务ID */
    private Long refId;

    /** 是否已读: 0-未读, 1-已读 */
    private Integer isRead;

}
