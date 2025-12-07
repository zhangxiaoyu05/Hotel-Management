package com.hotel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "文件资源", description = "文件静态资源访问接口")
public class FileResourceController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping("/upload-url/{fileName}")
    @Operation(summary = "访问上传的文件", description = "通过文件名访问上传的文件")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        try {
            // 默认在avatars目录中查找
            Path filePath = Paths.get(uploadDir, "avatars", fileName);

            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // 创建文件资源
            Resource resource = new FileSystemResource(filePath);

            // 获取文件内容类型
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            log.info("访问文件: {}", filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (IOException e) {
            log.error("访问文件失败: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{type}/{fileName}")
    @Operation(summary = "访问指定类型的文件", description = "通过类型和文件名访问上传的文件")
    public ResponseEntity<Resource> getFileByType(
            @PathVariable String type,
            @PathVariable String fileName) {
        try {
            // 验证文件类型
            String directory;
            switch (type.toLowerCase()) {
                case "hotel":
                    directory = "hotels";
                    break;
                case "avatar":
                default:
                    directory = "avatars";
                    break;
            }

            // 构建文件路径
            Path filePath = Paths.get(uploadDir, directory, fileName);

            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // 创建文件资源
            Resource resource = new FileSystemResource(filePath);

            // 获取文件内容类型
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            log.info("访问文件: {}, 类型: {}", filePath, type);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (IOException e) {
            log.error("访问文件失败: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}