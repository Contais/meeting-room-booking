package com.meetinghub.platform.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传响应
 * <p>
 * 二期：{@code url} 为上传后即时生成的预签名 URL（短期有效，用于前端即时预览），
 * {@code objectKey} 为对象键，前端提交业务表单时回传此值入库。
 * </p>
 */
@Data
public class FileUploadVO implements Serializable {

    /**
     * 可直接用于 {@code <img src>} 的访问 URL（预签名，短期有效）
     */
    private String url;

    /**
     * 对象键（存储相对路径），删除与入库时回传
     */
    private String objectKey;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件大小（字节）
     */
    private Long size;
}
