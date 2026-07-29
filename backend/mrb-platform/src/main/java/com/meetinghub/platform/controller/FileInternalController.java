package com.meetinghub.platform.controller;

import com.meetinghub.common.result.Result;
import com.meetinghub.platform.config.FileStorageProperties;
import com.meetinghub.platform.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件内部接口控制器
 * <p>
 * 仅供服务间 Feign 调用，绕过网关鉴权。提供批量预签名 URL 生成能力，
 * 供 mrb-user（avatar）、mrb-meeting（room image）读取侧动态签名使用。
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/file/internal")
public class FileInternalController {

    private final FileStorageService fileStorageService;
    private final FileStorageProperties properties;

    /**
     * 批量生成预签名 URL
     * <p>
     * 入参为 objectKey 列表，自动过滤掉 {@code http} 开头的旧数据（一期公开链接原样返回），
     * 仅对 objectKey 生成签名 URL。返回 Map：objectKey -> 签名 URL。
     * 调用方对未出现在返回 Map 中的字段保留原值（兼容旧 http 链接）。
     * </p>
     *
     * @param objectKeys 对象键列表
     * @return objectKey -> 预签名 URL
     */
    @PostMapping("/presigned-urls")
    public Result<Map<String, String>> batchPresignedUrls(@RequestBody List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return Result.ok(Collections.emptyMap());
        }
        long expire = properties.getCos().getPresignedExpire();
        Map<String, String> result = new HashMap<>();
        for (String key : objectKeys) {
            if (!StringUtils.hasText(key) || key.startsWith("http")) {
                // 旧数据（完整 URL）或空值跳过，调用方保留原值
                continue;
            }
            try {
                result.put(key, fileStorageService.generatePresignedUrl(key, expire));
            } catch (Exception e) {
                log.warn("批量生成预签名 URL 失败: objectKey={}", key, e);
            }
        }
        return Result.ok(result);
    }
}
