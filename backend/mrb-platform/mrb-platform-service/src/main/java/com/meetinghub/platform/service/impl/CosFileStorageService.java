package com.meetinghub.platform.service.impl;

import com.meetinghub.platform.config.FileStorageProperties;
import com.meetinghub.platform.service.FileStorageService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpMethodName;
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
import java.net.URL;
import java.util.Date;

/**
 * 腾讯云 COS 存储实现
 * <p>
 * 仅当 classpath 存在 COSClient 且 file.storage.type=cos 时启用。
 * 二期：桶保持私有，{@link #generatePresignedUrl} 按需生成带签名临时访问 URL。
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
        log.info("腾讯云 COS 存储已启用: bucket={}, region={}, presignedExpire={}s",
                cos.getBucket(), cos.getRegion(), cos.getPresignedExpire());
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
            // 二期：返回 objectKey 而非访问 URL，DB 只存 objectKey
            return objectKey;
        } catch (CosServiceException e) {
            log.error("COS 文件存储失败(服务端): objectKey={}, statusCode={}, errorCode={}, errorMessage={}, requestId={}, traceId={}",
                    objectKey, e.getStatusCode(), e.getErrorCode(), e.getErrorMessage(), e.getRequestId(), e.getTraceId(), e);
            throw new com.meetinghub.common.exception.BusinessException(com.meetinghub.common.exception.ErrorCode.FILE_UPLOAD_FAILED);
        } catch (CosClientException e) {
            log.error("COS 文件存储失败(客户端): objectKey={}", objectKey, e);
            throw new com.meetinghub.common.exception.BusinessException(com.meetinghub.common.exception.ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            cosClient.deleteObject(properties.getCos().getBucket(), objectKey);
        } catch (CosServiceException e) {
            log.warn("COS 文件删除失败(服务端): objectKey={}, statusCode={}, errorCode={}, requestId={}",
                    objectKey, e.getStatusCode(), e.getErrorCode(), e.getRequestId());
        } catch (CosClientException e) {
            log.warn("COS 文件删除失败(客户端): objectKey={}", objectKey, e);
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
    public String generatePresignedUrl(String objectKey, long expireSeconds) {
        if (isBlank(objectKey)) {
            return null;
        }
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
            URL url = cosClient.generatePresignedUrl(properties.getCos().getBucket(), objectKey, expiration, HttpMethodName.GET);
            return url.toString();
        } catch (CosClientException e) {
            log.error("COS 生成预签名 URL 失败: objectKey={}", objectKey, e);
            throw new com.meetinghub.common.exception.BusinessException(com.meetinghub.common.exception.ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public String type() {
        return "cos";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
