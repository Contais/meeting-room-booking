package com.meetinghub.platform.service;

import com.meetinghub.platform.enums.FileBizType;
import com.meetinghub.platform.model.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件业务服务：校验、路径生成、委托存储
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file    文件
     * @param bizType 业务类型
     * @return 上传结果（含即时预签名 URL 与 objectKey）
     */
    FileUploadVO upload(MultipartFile file, FileBizType bizType);

    /**
     * 删除文件
     *
     * @param objectKey 对象键
     */
    void delete(String objectKey);
}
