package com.meetinghub.platform.api.feign;

import com.meetinghub.platform.api.model.dto.NotificationSendDTO;
import com.meetinghub.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 站内信通知远程调用客户端
 * <p>
 * 通知域已迁至 mrb-platform，路径前缀 {@code /platform/internal/notification/**}。
 * 调用失败时降级为仅记录日志，不影响主业务流程。
 * </p>
 */
@FeignClient(name = "mrb-platform", contextId = "notificationFeignClient")
public interface NotificationFeignClient {

    @PostMapping("/platform/internal/notification/send")
    Result<Void> send(@RequestBody NotificationSendDTO dto);

    @PostMapping("/platform/internal/notification/send-batch")
    Result<Void> sendBatch(@RequestParam("userIds") List<Long> userIds, @RequestBody NotificationSendDTO template);
}
