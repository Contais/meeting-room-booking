package com.meetinghub.platform.api.mq.producer;

import com.meetinghub.platform.api.feign.NotificationFeignClient;
import com.meetinghub.platform.api.model.dto.NotificationSendDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 站内信通知发送外观（Facade）
 * <p>
 * 在 {@link NotificationProducer} 之上封装「MQ 优先 + Feign 降级 + 日志兜底」的容错策略，
 * 统一所有业务模块的通知发送入口，避免各 ServiceImpl 重复实现降级逻辑。
 * </p>
 * <ul>
 *   <li>正常链路：{@link NotificationProducer#send} 同步发送至 mrb-notification topic</li>
 *   <li>MQ 异常：降级 {@link NotificationFeignClient#sendBatch} 同步直发</li>
 *   <li>降级也失败：仅记录日志，不影响主业务（通知失败不回滚业务事务）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSender {

    private final NotificationProducer notificationProducer;
    private final NotificationFeignClient notificationFeignClient;

    /**
     * 安全发送批量站内信通知（容错：MQ 优先，失败降级 Feign 同步调用，再失败仅记录日志）
     *
     * @param userIds  接收人ID列表；为空直接返回
     * @param template 通知模板
     */
    public void sendSafe(List<Long> userIds, NotificationSendDTO template) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        try {
            notificationProducer.send(userIds, template);
        } catch (Exception e) {
            log.warn("MQ 投递失败，降级 Feign 同步发送, type={}", template.getType(), e);
            try {
                notificationFeignClient.sendBatch(userIds, template);
            } catch (Exception ex) {
                log.warn("降级 Feign 调用也失败, userIds={}, type={}", userIds, template.getType(), ex);
            }
        }
    }
}
