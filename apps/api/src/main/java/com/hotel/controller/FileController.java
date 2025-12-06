package com.hotel.controller;

import com.hotel.dto.ApiResponse;
import com.hotel.dto.UploadResponse;
import com.hotel.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "文件管理", description = "文件上传相关接口")
public class FileController {

    private final FileService fileService;

    // 支持的图片格式
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png"
    );

    // 最大文件大小 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传单个文件，支持图片格式")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadFile(
            @RequestParam("file") @NotNull MultipartFile file) {
        try {
            log.info("开始上传文件: {}", file.getOriginalFilename());

            // 验证文件是否为空
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文件不能为空"));
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("只支持 JPG、JPEG、PNG 格式的图片文件"));
            }

            // 验证文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文件大小不能超过 5MB"));
            }

            // 上传文件
            UploadResponse uploadResponse = fileService.uploadFile(file);

            log.info("文件上传成功: {}", file.getOriginalFilename());

            return ResponseEntity.ok(ApiResponse.success(uploadResponse, "文件上传成功"));

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文件上传失败: " + e.getMessage()));
        }
    }

    @GetMapping("/upload-url/{fileName}")
    @Operation(summary = "获取文件访问URL", description = "根据文件名获取文件的访问URL")
    public ResponseEntity<ApiResponse<String>> getFileUrl(@PathVariable String fileName) {
        try {
            String fileUrl = fileService.getFileUrl(fileName);
            return ResponseEntity.ok(ApiResponse.success(fileUrl, "获取文件URL成功"));
        } catch (Exception e) {
            log.error("获取文件URL失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取文件URL失败: " + e.getMessage()));
        }
    }
}