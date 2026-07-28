package com.meetinghub.meeting.feign;

import com.meetinghub.common.model.dto.NotificationSendDTO;
import com.meetinghub.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 站内信通知远程调用客户端
 * <p>
 * 仅供 mrb-meeting 内部调用 mrb-user 的通知发送接口。
 * 调用失败时降级为仅记录日志，不影响主业务流程。
 * </p>
 */
@FeignClient(name = "mrb-user")
public interface NotificationFeignClient {

    @PostMapping("/user/internal/notification/send")
    Result<Void> send(@RequestBody NotificationSendDTO dto);

    @PostMapping("/user/internal/notification/send-batch")
    Result<Void> sendBatch(@RequestParam("userIds") List<Long> userIds, @RequestBody NotificationSendDTO template);
}
