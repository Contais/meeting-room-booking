package com.meetinghub.platform.service.impl;

import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.platform.config.FileStorageProperties;
import com.meetinghub.platform.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 本地磁盘存储实现（默认，开发环境零配置可用）
 * <p>
 * 本地无鉴权概念，{@link #generatePresignedUrl} 直接返回静态访问 URL，与 COS 行为对齐。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final FileStorageProperties properties;

    private Path rootPath;

    @PostConstruct
    public void init() {
        rootPath = Paths.get(properties.getLocal().getRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
            log.info("本地文件存储根目录: {}", rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("初始化本地文件存储目录失败: " + rootPath, e);
        }
    }

    @Override
    public String store(byte[] content, String objectKey, String contentType) {
        try {
            Path target = rootPath.resolve(objectKey).normalize();
            if (!target.startsWith(rootPath)) {
                throw new BusinessException(ErrorCode.FILE_BIZ_TYPE_INVALID);
            }
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            // 二期：返回 objectKey 而非访问 URL，DB 只存 objectKey
            return objectKey;
        } catch (IOException e) {
            log.error("本地文件存储失败: objectKey={}", objectKey, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Path target = rootPath.resolve(objectKey).normalize();
            if (!target.startsWith(rootPath)) {
                return;
            }
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("本地文件删除失败: objectKey={}", objectKey, e);
        }
    }

    @Override
    public String getAccessUrl(String objectKey) {
        return buildStaticUrl(objectKey);
    }

    @Override
    public String generatePresignedUrl(String objectKey, long expireSeconds) {
        // 本地存储无鉴权，直接返回静态访问 URL，忽略有效期参数
        return buildStaticUrl(objectKey);
    }

    @Override
    public String type() {
        return "local";
    }

    private String buildStaticUrl(String objectKey) {
        String prefix = properties.getLocal().getUrlPrefix();
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix + objectKey;
    }

    /**
     * 供资源控制器读取本地文件使用
     */
    public Path resolve(String objectKey) {
        Path target = rootPath.resolve(objectKey).normalize();
        if (!target.startsWith(rootPath)) {
            return null;
        }
        return target;
    }
}
