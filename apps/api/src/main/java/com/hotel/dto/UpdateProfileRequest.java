package com.hotel.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;

    @Size(max = 50, message = "真实姓名长度不能超过 50 个字符")
    private String realName;

    private String gender;

    private String birthDate;

    private String avatar;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}