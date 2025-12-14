package com.hotel.service;

import com.hotel.entity.SystemConfig;
import com.hotel.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 系统备份服务
 *
 * @author System
 * @since 2025-12-14
 */
@Service
@Transactional
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private SystemSettingService systemSettingService;

    /**
     * 执行完整系统备份
     */
    public String executeFullBackup() {
        logger.info("开始执行完整系统备份");

        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = "hotel_backup_" + timestamp + ".zip";

            // 创建备份目录
            String backupDir = getBackupDirectory();
            Path backupPath = Paths.get(backupDir, backupFileName);

            // 确保备份目录存在
            Files.createDirectories(Paths.get(backupDir));

            // 执行备份
            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(backupPath.toFile()))) {

                // 1. 备份系统配置数据
                backupSystemConfig(zipOut, timestamp);

                // 2. 备份数据库（这里使用简化的方式，实际项目中应该使用数据库专用的备份工具）
                backupDatabase(zipOut, timestamp);

                // 3. 备份上传的文件
                backupUploadedFiles(zipOut, timestamp);

            }

            // 更新最后备份时间
            systemSettingService.saveOrUpdateConfig("backup.last_time", LocalDateTime.now().toString(),
                    "BACKUP", "最后备份时间", false, "system");

            logger.info("完整系统备份完成: {}", backupFileName);
            return backupFileName;

        } catch (Exception e) {
            logger.error("完整系统备份失败", e);
            throw new RuntimeException("备份失败: " + e.getMessage(), e);
        }
    }

    /**
     * 备份系统配置数据
     */
    private void backupSystemConfig(ZipOutputStream zipOut, String timestamp) throws IOException {
        logger.info("备份系统配置数据");

        // 创建配置备份文件
        Path configBackupPath = Paths.get(getBackupDirectory(), "system_config_" + timestamp + ".json");

        try (FileWriter writer = new FileWriter(configBackupPath.toFile())) {
            writer.write("{\n");
            writer.write("  \"backup_time\": \"" + LocalDateTime.now() + "\",\n");
            writer.write("  \"configs\": [\n");

            java.util.List<SystemConfig> configs = systemConfigRepository.findByConfigType("BASIC");
            configs.addAll(systemConfigRepository.findByConfigType("BUSINESS"));
            configs.addAll(systemConfigRepository.findByConfigType("NOTIFICATION"));
            configs.addAll(systemConfigRepository.findByConfigType("SECURITY"));
            configs.addAll(systemConfigRepository.findByConfigType("BACKUP"));

            for (int i = 0; i < configs.size(); i++) {
                SystemConfig config = configs.get(i);
                writer.write("    {\n");
                writer.write("      \"configKey\": \"" + config.getConfigKey() + "\",\n");
                writer.write("      \"configType\": \"" + config.getConfigType() + "\",\n");
                writer.write("      \"description\": \"" + config.getDescription() + "\",\n");
                writer.write("      \"isEncrypted\": " + config.getIsEncrypted() + ",\n");
                writer.write("      \"createdAt\": \"" + config.getCreatedAt() + "\",\n");
                writer.write("      \"updatedAt\": \"" + config.getUpdatedAt() + "\"\n");
                writer.write("    }");
                if (i < configs.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("  ]\n");
            writer.write("}\n");
        }

        // 添加到 ZIP 文件
        addToZip(zipOut, configBackupPath, "system_config.json");

        // 删除临时文件
        Files.deleteIfExists(configBackupPath);
    }

    /**
     * 备份数据库（简化版本）
     */
    private void backupDatabase(ZipOutputStream zipOut, String timestamp) throws IOException {
        logger.info("备份数据库结构");

        // 创建数据库结构备份文件
        Path dbBackupPath = Paths.get(getBackupDirectory(), "database_structure_" + timestamp + ".sql");

        try (FileWriter writer = new FileWriter(dbBackupPath.toFile())) {
            writer.write("-- Hotel Management System Database Backup\n");
            writer.write("-- Generated on: " + LocalDateTime.now() + "\n");
            writer.write("--\n");

            // 这里应该包含实际的数据库结构和数据导出
            // 为了演示，我们只写入一个结构说明
            writer.write("-- Database: hotel_management\n");
            writer.write("-- Tables: system_configs, system_config_audit, users, rooms, bookings, etc.\n");
            writer.write("-- Note: In production, use mysqldump or similar tools for full database backup\n");
        }

        // 添加到 ZIP 文件
        addToZip(zipOut, dbBackupPath, "database_structure.sql");

        // 删除临时文件
        Files.deleteIfExists(dbBackupPath);
    }

    /**
     * 备份上传的文件
     */
    private void backupUploadedFiles(ZipOutputStream zipOut, String timestamp) throws IOException {
        logger.info("备份上传文件");

        // 假设上传的文件存储在 uploads 目录
        Path uploadsDir = Paths.get("uploads");

        if (Files.exists(uploadsDir) && Files.isDirectory(uploadsDir)) {
            Files.walk(uploadsDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String relativePath = uploadsDir.relativize(path).toString();
                        addToZip(zipOut, path, "uploads/" + relativePath);
                    } catch (IOException e) {
                        logger.warn("跳过文件备份失败: {}", path, e);
                    }
                });
        }
    }

    /**
     * 将文件添加到 ZIP 压缩包
     */
    private void addToZip(ZipOutputStream zipOut, Path fileToAdd, String entryName) throws IOException {
        if (Files.exists(fileToAdd)) {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zipOut.putNextEntry(zipEntry);

            try (FileInputStream fis = new FileInputStream(fileToAdd.toFile())) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zipOut.write(buffer, 0, length);
                }
            }

            zipOut.closeEntry();
        }
    }

    /**
     * 获取备份目录
     */
    private String getBackupDirectory() {
        // 从配置中获取备份目录，如果未配置则使用默认目录
        try {
            SystemConfig backupConfig = systemConfigRepository.findByConfigKey("backup.directory")
                .orElse(null);
            if (backupConfig != null && backupConfig.getConfigValue() != null) {
                return backupConfig.getConfigValue();
            }
        } catch (Exception e) {
            logger.warn("获取备份目录配置失败，使用默认目录", e);
        }

        return "backups";
    }

    /**
     * 获取备份文件列表
     */
    @Transactional(readOnly = true)
    public java.util.List<String> getBackupFileList() {
        String backupDir = getBackupDirectory();
        Path backupPath = Paths.get(backupDir);

        if (!Files.exists(backupPath) || !Files.isDirectory(backupPath)) {
            return java.util.Collections.emptyList();
        }

        try {
            return Files.list(backupPath)
                .filter(path -> path.toString().endsWith(".zip"))
                .map(path -> path.getFileName().toString())
                .sorted((a, b) -> b.compareTo(a)) // 按文件名倒序排列（最新的在前）
                .limit(50) // 限制返回50个文件
                .collect(java.util.stream.Collectors.toList());
        } catch (IOException e) {
            logger.error("获取备份文件列表失败", e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 清理旧备份文件
     */
    public void cleanupOldBackups(int keepCount) {
        logger.info("清理旧备份文件，保留最新 {} 个", keepCount);

        java.util.List<String> backupFiles = getBackupFileList();
        if (backupFiles.size() > keepCount) {
            String backupDir = getBackupDirectory();

            for (int i = keepCount; i < backupFiles.size(); i++) {
                try {
                    Path oldBackup = Paths.get(backupDir, backupFiles.get(i));
                    Files.deleteIfExists(oldBackup);
                    logger.info("删除旧备份文件: {}", backupFiles.get(i));
                } catch (IOException e) {
                    logger.warn("删除旧备份文件失败: {}", backupFiles.get(i), e);
                }
            }
        }
    }
}