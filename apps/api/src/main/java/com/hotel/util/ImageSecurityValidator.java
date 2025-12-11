package com.hotel.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片安全验证工具类
 * 用于验证上传图片的安全性，防止恶意文件上传
 */
@Slf4j
@Component
public class ImageSecurityValidator {

    // 允许的图片扩展名
    private static final String[] ALLOWED_EXTENSIONS = {
        "jpg", "jpeg", "png", "gif", "webp", "bmp"
    };

    // 允许的MIME类型
    private static final String[] ALLOWED_MIME_TYPES = {
        "image/jpeg", "image/jpg", "image/png", "image/gif",
        "image/webp", "image/bmp"
    };

    // 危险文件扩展名（绝对禁止）
    private static final String[] DANGEROUS_EXTENSIONS = {
        "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js",
        "jar", "sh", "php", "asp", "aspx", "jsp", "py", "rb"
    };

    // 图片文件头签名（Magic Numbers）
    private static final Map<String, byte[]> IMAGE_SIGNATURES = new HashMap<>();

    static {
        IMAGE_SIGNATURES.put("jpg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF});
        IMAGE_SIGNATURES.put("png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47});
        IMAGE_SIGNATURES.put("gif", new byte[]{0x47, 0x49, 0x46, 0x38});
        IMAGE_SIGNATURES.put("webp", new byte[]{0x52, 0x49, 0x46, 0x46});
        IMAGE_SIGNATURES.put("bmp", new byte[]{0x42, 0x4D});
    }

    // 最大文件大小 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 最大图片尺寸
    private static final int MAX_IMAGE_WIDTH = 4096;
    private static final int MAX_IMAGE_HEIGHT = 4096;

    /**
     * 验证图片文件的安全性
     *
     * @param file 上传的文件
     * @return 验证结果
     */
    public ValidationResult validateImage(MultipartFile file) {
        try {
            // 1. 基本检查
            if (file == null || file.isEmpty()) {
                return ValidationResult.error("文件不能为空");
            }

            // 2. 文件大小检查
            if (file.getSize() > MAX_FILE_SIZE) {
                return ValidationResult.error("文件大小不能超过5MB");
            }

            // 3. 文件名检查
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                return ValidationResult.error("文件名不能为空");
            }

            // 4. 检查危险扩展名
            String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
            if (ArrayUtils.contains(DANGEROUS_EXTENSIONS, extension)) {
                log.warn("尝试上传危险文件: {}", originalFilename);
                return ValidationResult.error("不允许上传该类型的文件");
            }

            // 5. 检查是否为允许的扩展名
            if (!ArrayUtils.contains(ALLOWED_EXTENSIONS, extension)) {
                return ValidationResult.error("只支持jpg、png、gif、webp、bmp格式的图片");
            }

            // 6. MIME类型检查
            String mimeType = file.getContentType();
            if (mimeType == null || !ArrayUtils.contains(ALLOWED_MIME_TYPES, mimeType)) {
                return ValidationResult.error("文件类型不正确");
            }

            // 7. 扩展名与MIME类型一致性检查
            if (!isExtensionMatchMimeType(extension, mimeType)) {
                return ValidationResult.error("文件扩展名与文件类型不匹配");
            }

            // 8. 文件头签名验证
            byte[] fileBytes = file.getBytes();
            if (!isValidImageSignature(fileBytes, extension)) {
                log.warn("图片文件签名验证失败: {}", originalFilename);
                return ValidationResult.error("文件格式验证失败");
            }

            // 9. 尝试解析图片
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));
            if (image == null) {
                return ValidationResult.error("无法解析图片文件");
            }

            // 10. 图片尺寸检查
            if (image.getWidth() > MAX_IMAGE_WIDTH || image.getHeight() > MAX_IMAGE_HEIGHT) {
                return ValidationResult.error("图片尺寸不能超过4096x4096像素");
            }

            // 11. 检查是否包含EXIF数据（可选的安全检查）
            if (containsSuspiciousExifData(fileBytes)) {
                log.warn("图片包含可疑的EXIF数据: {}", originalFilename);
                // 可以选择清除EXIF数据或拒绝上传
            }

            return ValidationResult.success();

        } catch (IOException e) {
            log.error("图片验证时发生IO异常", e);
            return ValidationResult.error("文件读取失败");
        } catch (Exception e) {
            log.error("图片验证时发生异常", e);
            return ValidationResult.error("文件验证失败");
        }
    }

    /**
     * 检查扩展名与MIME类型是否匹配
     */
    private boolean isExtensionMatchMimeType(String extension, String mimeType) {
        Map<String, List<String>> extensionToMimeMap = new HashMap<>();
        extensionToMimeMap.put("jpg", Arrays.asList("image/jpeg", "image/jpg"));
        extensionToMimeMap.put("jpeg", Arrays.asList("image/jpeg", "image/jpg"));
        extensionToMimeMap.put("png", Arrays.asList("image/png"));
        extensionToMimeMap.put("gif", Arrays.asList("image/gif"));
        extensionToMimeMap.put("webp", Arrays.asList("image/webp"));
        extensionToMimeMap.put("bmp", Arrays.asList("image/bmp"));

        List<String> expectedMimeTypes = extensionToMimeMap.get(extension);
        return expectedMimeTypes != null && expectedMimeTypes.contains(mimeType);
    }

    /**
     * 验证图片文件头签名
     */
    private boolean isValidImageSignature(byte[] fileBytes, String extension) {
        byte[] expectedSignature = IMAGE_SIGNATURES.get(extension);
        if (expectedSignature == null || fileBytes.length < expectedSignature.length) {
            return false;
        }

        for (int i = 0; i < expectedSignature.length; i++) {
            if (fileBytes[i] != expectedSignature[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查是否包含可疑的EXIF数据
     * 这里是简化版本，实际项目中可以使用专门的EXIF处理库
     */
    private boolean containsSuspiciousExifData(byte[] fileBytes) {
        // 检查是否包含可疑的字符串
        String fileContent = new String(fileBytes, java.nio.charset.StandardCharsets.ISO_8859_1);

        // 检查是否包含脚本标签
        if (fileContent.toLowerCase().contains("<script")) {
            return true;
        }

        // 检查是否包含JavaScript代码
        if (fileContent.toLowerCase().contains("javascript:")) {
            return true;
        }

        // 检查是否包含PHP代码
        if (fileContent.toLowerCase().contains("<?php")) {
            return true;
        }

        return false;
    }

    /**
     * 生成安全的文件名
     */
    public String generateSafeFilename(String originalFilename, String prefix) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            originalFilename = "image";
        }

        String extension = FilenameUtils.getExtension(originalFilename);
        if (extension == null || extension.trim().isEmpty()) {
            extension = "jpg";
        }

        // 生成唯一文件名（时间戳 + 随机数）
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);

        return String.format("%s_%d_%d.%s", prefix, timestamp, random, extension);
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}