package com.meetinghub.platform.api.constant;

/**
 * RocketMQ 常量：Topic / Tag / ConsumerGroup 统一管理
 */
public final class MqConstant {

    private MqConstant() {
    }

    /**
     * 站内信通知 Topic
     */
    public static final String TOPIC_NOTIFICATION = "mrb-notification";

    /**
     * 预约事件 Topic（预留，暂未使用）
     */
    public static final String TOPIC_RESERVATION_EVENT = "mrb-reservation-event";

    /**
     * 站内信通知 ConsumerGroup（mrb-platform 消费）
     */
    public static final String CONSUMER_GROUP_NOTIFICATION = "mrb-platform-notification-consumer";
}
