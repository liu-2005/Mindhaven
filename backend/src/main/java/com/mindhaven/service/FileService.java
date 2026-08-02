package com.mindhaven.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.config.AppProperties;
import com.mindhaven.entity.FileObject;
import com.mindhaven.repository.FileObjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private final FileObjectRepository fileObjectRepository;
    private final AppProperties appProperties;

    public FileService(FileObjectRepository fileObjectRepository, AppProperties appProperties) {
        this.fileObjectRepository = fileObjectRepository;
        this.appProperties = appProperties;
    }

    @Transactional
    public FileObject upload(Long ownerId, MultipartFile file, String businessType, Long businessId, String businessField) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE, "文件不能超过 5MB");
        }
        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalName.substring(dot);
        }
        String storageKey = java.time.LocalDate.now() + "/" + UUID.randomUUID() + ext;
        byte[] bytes;
        String sha256;
        try {
            bytes = file.getBytes();
            sha256 = sha256(bytes);
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "读取文件失败");
        }

        Path target = storagePath(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "保存文件失败");
        }

        FileObject obj = new FileObject();
        obj.setStorageProvider("local");
        obj.setStorageKey(storageKey);
        obj.setOriginalName(originalName);
        obj.setContentType(contentType);
        obj.setSizeBytes((long) bytes.length);
        obj.setSha256(sha256);
        obj.setBusinessType(businessType);
        obj.setBusinessId(businessId);
        obj.setBusinessField(businessField);
        obj.setOwnerUserId(ownerId);
        obj.setStatus("BOUND");
        return fileObjectRepository.save(obj);
    }

    public FileObject get(Long fileId) {
        return fileObjectRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public byte[] readBytes(FileObject file) {
        Path path = storagePath(file.getStorageKey());
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "文件内容不可用");
        }
    }

    @Transactional
    public void delete(Long fileId) {
        FileObject file = get(fileId);
        file.setDeletedAt(java.time.LocalDateTime.now());
        file.setStatus("DELETED");
        fileObjectRepository.save(file);
    }

    private Path storagePath(String storageKey) {
        return Path.of(appProperties.getStorageDir()).resolve(storageKey).toAbsolutePath().normalize();
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("sha256 failed", e);
        }
    }
}
