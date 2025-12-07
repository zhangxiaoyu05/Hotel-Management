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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "文件管理", description = "文件上传相关接口")
public class FileController {

    private final FileService fileService;

    // 支持的图片格式
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    // 文件名安全模式 - 防止路径遍历攻击
    private static final java.util.regex.Pattern SAFE_FILENAME_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9._-]+$");

    // 最大文件大小
    private static final long MAX_FILE_SIZE_AVATAR = 5 * 1024 * 1024;      // 5MB
    private static final long MAX_FILE_SIZE_HOTEL = 10 * 1024 * 1024;     // 10MB
    private static final long MAX_FILE_SIZE_ROOMTYPE = 5 * 1024 * 1024;  // 5MB

    // 上传类型枚举
    private enum UploadType {
        AVATAR("avatar", MAX_FILE_SIZE_AVATAR, "5MB", false),
        HOTEL("hotel", MAX_FILE_SIZE_HOTEL, "10MB", true),
        ROOMTYPE("roomtype", MAX_FILE_SIZE_ROOMTYPE, "5MB", true);

        private final String typeName;
        private final long maxSize;
        private final String sizeText;
        private final boolean requiresAdmin;

        UploadType(String typeName, long maxSize, String sizeText, boolean requiresAdmin) {
            this.typeName = typeName;
            this.maxSize = maxSize;
            this.sizeText = sizeText;
            this.requiresAdmin = requiresAdmin;
        }

        public static UploadType fromString(String type) {
            if (type == null) return AVATAR;
            try {
                return UploadType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return AVATAR;
            }
        }
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传单个文件到指定类型目录，支持图片格式")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadFile(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam(value = "type", defaultValue = "avatar") String type) {
        try {
            // 验证上传类型
            UploadType uploadType = UploadType.fromString(type);

            // 验证权限 - 需要管理员权限的类型
            if (uploadType.requiresAdmin) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                    log.warn("非管理员用户尝试上传受限文件类型: {}, 用户: {}", type,
                            auth != null ? auth.getName() : "anonymous");
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("此文件类型需要管理员权限"));
                }
            }

            log.info("用户 {} 开始上传文件: {}, 类型: {}",
                    getCurrentUser(), file.getOriginalFilename(), type);

            // 验证文件是否为空
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文件不能为空"));
            }

            // 验证文件名安全性
            String originalFilename = file.getOriginalFilename();
            if (!isSafeFilename(originalFilename)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文件名包含非法字符"));
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                log.warn("不允许的文件类型: {}, 用户: {}", contentType, getCurrentUser());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("只支持 JPG、JPEG、PNG、GIF、WEBP、SVG 格式的图片文件"));
            }

            // 验证文件大小
            if (file.getSize() > uploadType.maxSize) {
                log.warn("文件大小超出限制: {}, 用户: {}, 大小: {}, 限制: {}",
                        getCurrentUser(), file.getSize(), uploadType.maxSize);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文件大小不能超过 " + uploadType.sizeText));
            }

            // 额外的文件内容验证 - 检查魔数防止伪造扩展名
            if (!isValidImageFile(file)) {
                log.warn("检测到无效的图片文件: {}, 用户: {}", originalFilename, getCurrentUser());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文件内容与扩展名不匹配，请上传有效的图片文件"));
            }

            // 上传文件
            UploadResponse uploadResponse = fileService.uploadFile(file, uploadType.typeName);

            log.info("文件上传成功: {}, 类型: {}, 用户: {}",
                    originalFilename, type, getCurrentUser());

            return ResponseEntity.ok(ApiResponse.success(uploadResponse, "文件上传成功"));

        } catch (Exception e) {
            log.error("文件上传失败: {}, 用户: {}, 错误: {}",
                    file.getOriginalFilename(), getCurrentUser(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文件上传失败: " + e.getMessage()));
        }
    }

    @GetMapping("/upload-url/{fileName}")
    @Operation(summary = "获取文件访问URL", description = "根据文件名获取文件的访问URL")
    public ResponseEntity<ApiResponse<String>> getFileUrl(@PathVariable String fileName) {
        try {
            // 验证文件名安全性
            if (!isSafeFilename(fileName)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文件名包含非法字符"));
            }

            String fileUrl = fileService.getFileUrl(fileName);
            return ResponseEntity.ok(ApiResponse.success(fileUrl, "获取文件URL成功"));
        } catch (Exception e) {
            log.error("获取文件URL失败: {}, 用户: {}", fileName, getCurrentUser(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取文件URL失败: " + e.getMessage()));
        }
    }

    /**
     * 验证文件名是否安全
     */
    private boolean isSafeFilename(String filename) {
        if (StringUtils.isBlank(filename)) {
            return false;
        }

        // 检查路径遍历攻击
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }

        // 检查文件名长度
        if (filename.length() > 255) {
            return false;
        }

        // 检查是否只包含安全字符
        String nameWithoutExt = filename.contains(".") ?
                filename.substring(0, filename.lastIndexOf('.')) : filename;

        return SAFE_FILENAME_PATTERN.matcher(nameWithoutExt).matches();
    }

    /**
     * 验证是否为有效的图片文件
     */
    private boolean isValidImageFile(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 8) {
                return false;
            }

            // 检查常见图片格式的魔数
            String contentType = file.getContentType();

            if (contentType != null) {
                switch (contentType.toLowerCase()) {
                    case "image/jpeg":
                    case "image/jpg":
                        // JPEG 魔数: FF D8 FF
                        return bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF;

                    case "image/png":
                        // PNG 魔数: 89 50 4E 47 0D 0A 1A 0A
                        return bytes.length >= 8 &&
                               bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47 &&
                               bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;

                    case "image/gif":
                        // GIF 魔数: 47 49 46 38 (GIF8)
                        return bytes.length >= 6 &&
                               bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38;

                    case "image/webp":
                        // WebP 魔数: 52 49 46 46 ... 57 45 42 50 (RIFF....WEBP)
                        return bytes.length >= 12 &&
                               bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46 &&
                               bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50;

                    case "image/svg+xml":
                        // SVG 文件应该是文本格式，检查是否包含 SVG 标记
                        String content = new String(bytes, "UTF-8");
                        return content.toLowerCase().contains("<svg") && content.toLowerCase().contains("</svg>");
                }
            }

            return false;
        } catch (Exception e) {
            log.warn("验证图片文件时出错: {}, 错误: {}", file.getOriginalFilename(), e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前用户名
     */
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }
}