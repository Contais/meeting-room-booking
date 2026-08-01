package com.meetinghub.platform.mq.consumer;

import com.meetinghub.platform.api.constant.MqConstant;
import com.meetinghub.common.constant.RedisKeyConstant;
import com.meetinghub.platform.api.model.dto.NotificationMessage;
import com.meetinghub.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 站内信通知 MQ 消费者
 * <p>
 * 消费 {@link MqConstant#TOPIC_NOTIFICATION} 消息，落库 + WebSocket 推送。
 * 幂等性（红线 #3）：以 {@code mrb:mq:dedup:mrb-notification:{msgKey}} 做去重，
 * 同一 msgKey 24 小时内仅处理一次，RocketMQ 重投递无副作用。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqConstant.TOPIC_NOTIFICATION,
        consumerGroup = MqConstant.CONSUMER_GROUP_NOTIFICATION
)
public class NotificationConsumer implements RocketMQListener<NotificationMessage> {

    private static final long DEDUP_TTL_HOURS = 24L;

    private final NotificationService notificationService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(NotificationMessage message) {
        String msgKey = message.getMsgKey();
        if (msgKey == null || msgKey.isBlank()) {
            log.warn("[NotificationConsumer] 消息缺少 msgKey，跳过, userIds={}", message.getUserIds());
            return;
        }
        String dedupKey = RedisKeyConstant.MQ_DEDUP + MqConstant.TOPIC_NOTIFICATION + ":" + msgKey;
        // CAS 抢占：setIfAbsent 原子操作，抢到才处理
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofHours(DEDUP_TTL_HOURS));
        if (Boolean.FALSE.equals(acquired)) {
            log.info("[NotificationConsumer] 消息已处理，幂等跳过, msgKey={}", msgKey);
            return;
        }
        try {
            notificationService.sendBatch(message.getUserIds(), message.getTemplate());
            log.info("[NotificationConsumer] 通知消费完成, msgKey={}", msgKey);
        } catch (Exception e) {
            // 处理失败：删除去重标记，允许 MQ 重投递后重新消费
            stringRedisTemplate.delete(dedupKey);
            log.error("[NotificationConsumer] 通知消费失败，已释放去重标记等待重试, msgKey={}", msgKey, e);
            throw e;
        }
    }
}
