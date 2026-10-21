package com.mindhaven.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

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
        if (!"ARTICLE".equals(businessType)) throw new BizException(ErrorCode.VALIDATION_ERROR, "当前仅支持上传公开文章图片");
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
        try {
            byte[] safeImage = normalizeImage(file.getBytes());
            return store(ownerId, safeImage, "image.png", "image/png", businessType, businessId, businessField);
        } catch (IOException e) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "无法读取图片，请使用完整的PNG或JPEG文件");
        }
    }

    private byte[] normalizeImage(byte[] bytes) throws IOException {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BizException(ErrorCode.VALIDATION_ERROR, "仅支持有效的 PNG、JPEG 图片");
            var reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!java.util.Set.of("png", "jpeg", "jpg").contains(format)) {
                    throw new BizException(ErrorCode.VALIDATION_ERROR, "仅支持 PNG、JPEG 图片");
                }
                reader.setInput(input, true, true);
                if (reader.getWidth(0) > 4096 || reader.getHeight(0) > 4096) {
                    throw new BizException(ErrorCode.VALIDATION_ERROR, "图片长宽不能超过4096像素");
                }
                var image = reader.read(0);
                var output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                if (output.size() > MAX_SIZE) throw new BizException(ErrorCode.FILE_TOO_LARGE, "处理后的图片不能超过5MB");
                return output.toByteArray();
            } finally { reader.dispose(); }
        }
    }

    /**
     * 直接存储字节内容（供服务端生成/种子数据使用，跳过大小与 MIME 校验）。
     */
    public FileObject store(Long ownerId, byte[] bytes, String originalName, String contentType,
                            String businessType, Long businessId, String businessField) {
        if (bytes == null || bytes.length == 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "文件内容为空");
        }
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalName.substring(dot);
        }
        String storageKey = java.time.LocalDate.now() + "/" + UUID.randomUUID() + ext;
        String sha256 = sha256(bytes);

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
        Path root = Path.of(appProperties.getStorageDir()).toAbsolutePath().normalize();
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) throw new BizException(ErrorCode.FORBIDDEN, "非法文件路径");
        return target;
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
