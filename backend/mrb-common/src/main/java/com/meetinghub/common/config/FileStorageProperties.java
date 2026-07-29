package com.meetinghub.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置
 * <p>
 * 通过 file.storage.* 统一配置，切换 type 即可在本地 / 腾讯 COS 之间无感切换。
 * 后续接入阿里 OSS 只需新增对应实现并在 type 中指定。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /**
     * 存储类型：local / cos，默认 local
     */
    private String type = "local";

    /**
     * 本地存储配置
     */
    private Local local = new Local();

    /**
     * 腾讯云 COS 配置
     */
    private Cos cos = new Cos();

    @Data
    public static class Local {
        /**
         * 本地存储根目录
         */
        private String root = "./uploads";

        /**
         * 访问 URL 前缀（浏览器可访问的路径前缀）
         */
        private String urlPrefix = "/api/file/static";
    }

    @Data
    public static class Cos {
        /**
         * SecretId
         */
        private String secretId;

        /**
         * SecretKey
         */
        private String secretKey;

        /**
         * 存储桶名称，格式 BucketName-APPID
         */
        private String bucket;

        /**
         * 地域简称，如 ap-guangzhou
         */
        private String region;

        /**
         * 自定义访问域名前缀（含 https），为空时使用默认 COS 域名
         */
        private String domain;
    }
}
