package com.meetinghub.common.service;

/**
 * 文件存储抽象层
 * <p>
 * 屏蔽底层存储差异：本地磁盘 / 腾讯 COS / 阿里 OSS。
 * 切换存储实现只需更换 {@code file.storage.type} 配置，调用方无感知。
 * </p>
 */
public interface FileStorageService {

    /**
     * 存储文件
     *
     * @param content     文件字节内容
     * @param objectKey   对象键（相对路径，如 avatar/202607/uuid.png）
     * @param contentType MIME 类型
     * @return 可访问的 URL
     */
    String store(byte[] content, String objectKey, String contentType);

    /**
     * 删除文件
     *
     * @param objectKey 对象键
     */
    void delete(String objectKey);

    /**
     * 获取访问 URL
     *
     * @param objectKey 对象键
     * @return 访问 URL
     */
    String getAccessUrl(String objectKey);

    /**
     * 当前存储类型标识，便于日志与排障
     *
     * @return 类型名（local / cos）
     */
    String type();
}
