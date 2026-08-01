package com.meetinghub.platform.controller;

import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.common.result.Result;
import com.meetinghub.platform.enums.FileBizType;
import com.meetinghub.platform.model.vo.FileUploadVO;
import com.meetinghub.platform.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件控制器
 * <p>
 * 统一文件上传/删除接口，由 mrb-platform 通过网关路由暴露。
 * 上传需登录鉴权（网关 AuthGlobalFilter 已校验）。
 * </p>
 */
@RestController
@RequestMapping("/platform/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件
     *
     * @param file    文件
     * @param bizType 业务类型（AVATAR / ROOM_IMAGE）
     */
    @PostMapping("/upload")
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam("bizType") String bizType) {
        FileBizType type = FileBizType.of(bizType);
        if (type == null) {
            throw new BusinessException(ErrorCode.FILE_BIZ_TYPE_INVALID);
        }
        return Result.ok(fileService.upload(file, type));
    }

    /**
     * 删除文件
     *
     * @param objectKey 对象键
     */
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam("objectKey") String objectKey) {
        fileService.delete(objectKey);
        return Result.ok();
    }
}
