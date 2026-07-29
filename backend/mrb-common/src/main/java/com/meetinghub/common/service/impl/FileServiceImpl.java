package com.meetinghub.common.service.impl;

import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.common.enums.FileBizType;
import com.meetinghub.common.model.vo.FileUploadVO;
import com.meetinghub.common.service.FileService;
import com.meetinghub.common.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;

/**
 * 文件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final long MAX_SIZE = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final FileStorageService fileStorageService;

    @Override
    public FileUploadVO upload(MultipartFile file, FileBizType bizType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        String ext = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        String objectKey = buildObjectKey(bizType, ext);
        try {
            String url = fileStorageService.store(file.getBytes(), objectKey, file.getContentType());
            FileUploadVO vo = new FileUploadVO();
            vo.setUrl(url);
            vo.setObjectKey(objectKey);
            vo.setBizType(bizType.name());
            vo.setOriginalName(file.getOriginalFilename());
            vo.setSize(file.getSize());
            log.info("文件上传成功: bizType={}, objectKey={}, storage={}", bizType, objectKey, fileStorageService.type());
            return vo;
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        fileStorageService.delete(objectKey);
    }

    private String buildObjectKey(FileBizType bizType, String ext) {
        return bizType.getPath() + "/" + YearMonth.now() + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase();
    }
}
