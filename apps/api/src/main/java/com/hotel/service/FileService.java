package com.hotel.service;

import com.hotel.dto.UploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://localhost:8080/api/files}")
    private String baseUrl;

    // 头像上传目录
    private static final String AVATAR_DIR = "avatars";
    // 酒店图片上传目录
    private static final String HOTEL_DIR = "hotels";

    /**
     * 上传文件（默认头像）
     */
    public UploadResponse uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, AVATAR_DIR);
    }

    /**
     * 上传文件到指定目录
     */
    public UploadResponse uploadFile(MultipartFile file, String type) throws IOException {
        String directory;
        switch (type.toLowerCase()) {
            case "hotel":
                directory = HOTEL_DIR;
                break;
            case "avatar":
            default:
                directory = AVATAR_DIR;
                break;
        }

        // 确保上传目录存在
        Path uploadPath = Paths.get(uploadDir, directory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成唯一的文件名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFilename = generateUniqueFilename(extension);

        // 保存文件
        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath);

        // 构建响应
        UploadResponse response = new UploadResponse();
        response.setUrl(baseUrl + "/" + directory + "/" + newFilename);
        response.setFilename(newFilename);
        response.setOriginalFilename(originalFilename);
        response.setContentType(file.getContentType());
        response.setSize(file.getSize());
        response.setType(type);

        log.info("文件保存成功: {}", filePath);

        return response;
    }

    /**
     * 获取文件访问URL
     */
    public String getFileUrl(String fileName) {
        // 检查文件是否存在
        Path filePath = Paths.get(uploadDir, AVATAR_DIR, fileName);
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在");
        }

        return baseUrl + "/upload-url/" + fileName;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 生成唯一的文件名
     */
    private String generateUniqueFilename(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "");

        return String.format("%s_%s.%s", timestamp, uuid, extension);
    }

    /**
     * 计算文件的MD5哈希值
     */
    private String calculateMD5(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(file.getBytes());
        StringBuilder hexString = new StringBuilder();

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir, AVATAR_DIR, fileName);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.info("文件删除成功: {}", filePath);
            } else {
                log.warn("文件不存在，无需删除: {}", filePath);
            }

            return deleted;
        } catch (IOException e) {
            log.error("删除文件失败: {}", e.getMessage(), e);
            return false;
        }
    }
}