package com.meetinghub.common.service.impl;

import com.meetinghub.common.config.FileStorageProperties;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.common.service.FileStorageService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

/**
 * 腾讯云 COS 存储实现
 * <p>
 * 仅当 classpath 存在 COSClient 且 file.storage.type=cos 时启用。
 * 切换为阿里 OSS 时新增 OssFileStorageService 实现并将 type 指定为 oss 即可。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnClass(COSClient.class)
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "cos")
public class CosFileStorageService implements FileStorageService {

    private final FileStorageProperties properties;

    private COSClient cosClient;

    @PostConstruct
    public void init() {
        FileStorageProperties.Cos cos = properties.getCos();
        if (isBlank(cos.getSecretId()) || isBlank(cos.getSecretKey())
                || isBlank(cos.getBucket()) || isBlank(cos.getRegion())) {
            throw new IllegalStateException("启用 COS 存储需配置 file.storage.cos 的 secretId/secretKey/bucket/region");
        }
        COSCredentials cred = new BasicCOSCredentials(cos.getSecretId(), cos.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cos.getRegion()));
        this.cosClient = new COSClient(cred, clientConfig);
        log.info("腾讯云 COS 存储已启用: bucket={}, region={}", cos.getBucket(), cos.getRegion());
    }

    @PreDestroy
    public void destroy() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }

    @Override
    public String store(byte[] content, String objectKey, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.length);
            if (!isBlank(contentType)) {
                metadata.setContentType(contentType);
            }
            PutObjectRequest request = new PutObjectRequest(
                    properties.getCos().getBucket(),
                    objectKey,
                    new ByteArrayInputStream(content),
                    metadata);
            cosClient.putObject(request);
            return getAccessUrl(objectKey);
        } catch (CosClientException e) {
            log.error("COS 文件存储失败: objectKey={}", objectKey, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            cosClient.deleteObject(properties.getCos().getBucket(), objectKey);
        } catch (CosClientException e) {
            log.warn("COS 文件删除失败: objectKey={}", objectKey, e);
        }
    }

    @Override
    public String getAccessUrl(String objectKey) {
        FileStorageProperties.Cos cos = properties.getCos();
        if (!isBlank(cos.getDomain())) {
            String domain = cos.getDomain();
            if (!domain.endsWith("/")) {
                domain = domain + "/";
            }
            return domain + objectKey;
        }
        // 默认域名：https://{bucket}.cos.{region}.myqcloud.com/{objectKey}
        return "https://" + cos.getBucket() + ".cos." + cos.getRegion() + ".myqcloud.com/" + objectKey;
    }

    @Override
    public String type() {
        return "cos";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
