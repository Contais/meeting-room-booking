package com.meetinghub.common.model.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 站内信 MQ 消息体（Producer → Consumer 传输）
 * <p>
 * 包装批量接收人与通知模板，附带 {@link #msgKey} 用于消费端幂等去重。
 * </p>
 */
@Data
public class NotificationMessage implements Serializable {

    /**
     * 消息业务唯一键（UUID），用于消费端 Redis 幂等去重
     */
    private String msgKey;

    /**
     * 接收人ID列表
     */
    private List<Long> userIds;

    /**
     * 通知模板（type/title/content/refType/refId）
     */
    private NotificationSendDTO template;
}
