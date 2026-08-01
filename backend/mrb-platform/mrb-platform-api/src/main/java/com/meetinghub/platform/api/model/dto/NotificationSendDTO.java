package com.meetinghub.platform.api.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 站内信发送 DTO（跨服务 Feign 调用使用）
 */
@Data
public class NotificationSendDTO implements Serializable {
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
}
