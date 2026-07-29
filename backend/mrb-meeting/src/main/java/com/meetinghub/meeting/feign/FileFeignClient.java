package com.meetinghub.meeting.feign;

import com.meetinghub.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * 文件预签名远程调用客户端
 * <p>
 * 供 mrb-meeting 读取侧（如会议室图片）将 objectKey 动态转为预签名 URL。
 * 调用失败时降级返回空 Map，调用方保留原值。
 * </p>
 */
@FeignClient(name = "mrb-platform", contextId = "fileFeignClient")
public interface FileFeignClient {

    /**
     * 批量生成预签名 URL
     *
     * @param objectKeys 对象键列表
     * @return objectKey -> 预签名 URL
     */
    @PostMapping("/platform/file/internal/presigned-urls")
    Result<Map<String, String>> batchPresignedUrls(@RequestBody List<String> objectKeys);
}
