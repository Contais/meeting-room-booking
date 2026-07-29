package com.meetinghub.common.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传响应
 */
@Data
public class FileUploadVO implements Serializable {

    /**
     * 可直接用于 <img src> 的访问 URL
     */
    private String url;

    /**
     * 对象键（存储相对路径），删除时回传
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
