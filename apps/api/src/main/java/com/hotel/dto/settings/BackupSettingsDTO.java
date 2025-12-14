package com.hotel.dto.settings;

import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.time.LocalDateTime;

@Data
public class BackupSettingsDTO {

    private Long id;

    private Boolean enableAutoBackup;

    @Min(value = 1, message = "备份间隔天数不能小于1")
    @Max(value = 30, message = "备份间隔天数不能超过30")
    private Integer backupIntervalDays;

    @Min(value = 0, message = "备份保留天数不能小于0")
    @Max(value = 365, message = "备份保留天数不能超过365")
    private Integer backupRetentionDays;

    private String backupTime;

    private String backupStorageType;

    private String localBackupPath;

    private String cloudBackupProvider;

    private String cloudBackupBucket;

    private String cloudBackupRegion;

    private String cloudAccessKey;

    private String cloudSecretKey;

    private Boolean enableBackupEncryption;

    private String backupEncryptionKey;

    private Boolean enableDatabaseBackup;

    private Boolean enableFileBackup;

    private String backupCompressionType;

    private LocalDateTime lastBackupTime;

    private LocalDateTime nextBackupTime;

    private LocalDateTime updatedAt;

    private String updatedBy;
}