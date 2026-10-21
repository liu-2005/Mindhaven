package com.mindhaven.controller;

import java.util.Map;

import com.mindhaven.common.ApiResponse;
import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.entity.FileObject;
import com.mindhaven.security.AuthContextHolder;
import com.mindhaven.security.RequirePermission;
import com.mindhaven.service.FileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    @RequirePermission("article:manage")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(required = false) String businessType,
                                                   @RequestParam(required = false) Long businessId,
                                                   @RequestParam(required = false) String businessField) {
        FileObject saved = fileService.upload(AuthContextHolder.userId(), file, businessType, businessId, businessField);
        String url = "/api/v1/files/" + saved.getId();
        return ApiResponse.success(Map.of("fileId", saved.getId(), "filePath", url,
                "url", url, "originalName", saved.getOriginalName()));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<byte[]> download(@PathVariable Long fileId) {
        FileObject file = fileService.get(fileId);
        if (!"ARTICLE".equals(file.getBusinessType())) throw new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        byte[] bytes = fileService.readBytes(file);
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", java.util.Set.of("image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml").contains(contentType) ? "inline" : "attachment")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "sandbox; default-src 'none'; style-src 'unsafe-inline'")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    @DeleteMapping("/{fileId}")
    @RequirePermission("article:manage")
    public ApiResponse<Void> delete(@PathVariable Long fileId) {
        FileObject file = fileService.get(fileId);
        Long owner = file.getOwnerUserId();
        if (owner != null && !owner.equals(AuthContextHolder.userId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        fileService.delete(fileId);
        return ApiResponse.success(null);
    }
}
