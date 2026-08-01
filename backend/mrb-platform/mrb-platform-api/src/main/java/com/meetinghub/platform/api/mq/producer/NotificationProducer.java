package com.meetinghub.platform.api.mq.producer;

import com.meetinghub.platform.api.constant.MqConstant;
import com.meetinghub.platform.api.model.dto.NotificationMessage;
import com.meetinghub.platform.api.model.dto.NotificationSendDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 站内信通知 MQ 生产者
 * <p>
 * 各业务模块通过本生产者将通知投递至 {@link MqConstant#TOPIC_NOTIFICATION}，
 * 由 mrb-platform 异步消费落库 + WebSocket 推送，实现主业务与通知发送解耦。
 * </p>
 * <p>
 * 仅在配置了 {@code rocketmq.name-server} 的服务中装配（与 RocketMQAutoConfiguration 条件一致），
 * 避免未配置 RocketMQ 的服务启动时因缺少 {@link RocketMQTemplate} bean 而失败。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RequiredArgsConstructor
public class NotificationProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 同步发送批量站内信通知
     *
     * @param userIds  接收人ID列表
     * @param template 通知模板
     */
    public void send(List<Long> userIds, NotificationSendDTO template) {
        NotificationMessage message = new NotificationMessage();
        message.setMsgKey(UUID.randomUUID().toString());
        message.setUserIds(userIds);
        message.setTemplate(template);
        // 同步发送：保证消息可靠投递，失败时抛异常由调用方降级
        rocketMQTemplate.syncSend(MqConstant.TOPIC_NOTIFICATION, MessageBuilder.withPayload(message).build());
        log.info("[NotificationProducer] 通知消息已投递, msgKey={}, userIds={}, type={}",
                message.getMsgKey(), userIds, template.getType());
    }
}
