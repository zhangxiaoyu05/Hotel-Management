package com.hotel.dto.settings;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class BasicSettingsDTO {

    private Long id;

    @NotBlank(message = "系统名称不能为空")
    @Size(max = 100, message = "系统名称长度不能超过100个字符")
    private String systemName;

    @Size(max = 500, message = "系统Logo URL长度不能超过500个字符")
    private String systemLogo;

    @Size(max = 50, message = "联系电话长度不能超过50个字符")
    private String contactPhone;

    @Size(max = 100, message = "联系邮箱长度不能超过100个字符")
    private String contactEmail;

    @Size(max = 200, message = "联系地址长度不能超过200个字符")
    private String contactAddress;

    @Size(max = 1000, message = "系统描述长度不能超过1000个字符")
    private String systemDescription;

    private String businessHours;

    private LocalDateTime updatedAt;

    private String updatedBy;
}