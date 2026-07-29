package com.meetinghub.platform.controller;

import com.meetinghub.platform.service.impl.LocalFileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 本地文件资源访问控制器
 * <p>
 * 仅在本地存储模式启用，对外提供 /file/static/** 静态访问，
 * 供浏览器 {@code <img src>} 直接加载（网关已放行该前缀的匿名访问）。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/file/static")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileResourceController {

    private final LocalFileStorageService localFileStorageService;

    @GetMapping("/**")
    public ResponseEntity<Resource> serve(HttpServletRequest request) {
        String objectKey = extractObjectKey(request.getRequestURI());
        Path file = localFileStorageService.resolve(objectKey);
        if (file == null || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        try {
            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                    .body(new FileSystemResource(file));
        } catch (Exception e) {
            log.warn("读取本地文件失败: objectKey={}", objectKey, e);
            return ResponseEntity.notFound().build();
        }
    }

    private String extractObjectKey(String requestUri) {
        int idx = requestUri.indexOf("/file/static/");
        if (idx < 0) {
            return "";
        }
        return requestUri.substring(idx + "/file/static/".length());
    }
}
