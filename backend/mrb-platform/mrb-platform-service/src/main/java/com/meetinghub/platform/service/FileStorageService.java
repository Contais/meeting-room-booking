package com.meetinghub.platform.service;

/**
 * 文件存储抽象层
 * <p>
 * 屏蔽底层存储差异：本地磁盘 / 腾讯 COS / 阿里 OSS。
 * 切换存储实现只需更换 {@code file.storage.type} 配置，调用方无感知。
 * </p>
 * <p>
 * 二期语义变更（COS 私有桶 + 预签名）：
 * <ul>
 *   <li>{@link #store} 返回 objectKey（相对路径），不再返回访问 URL；DB 只存 objectKey</li>
 *   <li>读取侧通过 {@link #generatePresignedUrl} 动态生成带签名的临时访问 URL</li>
 * </ul>
 * </p>
 */
public interface FileStorageService {

    /**
     * 存储文件
     *
     * @param content     文件字节内容
     * @param objectKey   对象键（相对路径，如 avatar/202607/uuid.png）
     * @param contentType MIME 类型
     * @return 实际写入的 objectKey（与入参一致，便于链式调用）
     */
    String store(byte[] content, String objectKey, String contentType);

    /**
     * 删除文件
     *
     * @param objectKey 对象键
     */
    void delete(String objectKey);

    /**
     * 获取直接访问 URL（本地模式静态 URL，或 COS 公开桶 URL）
     * <p>
     * 私有桶场景不应使用此方法，应改用 {@link #generatePresignedUrl}。
     * </p>
     *
     * @param objectKey 对象键
     * @return 访问 URL
     */
    String getAccessUrl(String objectKey);

    /**
     * 生成预签名访问 URL（私有桶按需授权）
     * <p>
     * 本地模式无鉴权概念，直接返回静态访问 URL；COS 模式生成带签名的临时 URL。
     * </p>
     *
     * @param objectKey     对象键
     * @param expireSeconds 有效期秒数
     * @return 预签名访问 URL
     */
    String generatePresignedUrl(String objectKey, long expireSeconds);

    /**
     * 当前存储类型标识，便于日志与排障
     *
     * @return 类型名（local / cos）
     */
    String type();
}
