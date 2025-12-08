package com.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求DTO
 * 支持用户名、邮箱或手机号登录
 */
public class LoginRequest {

    @NotBlank(message = "登录账号不能为空")
    @Size(max = 100, message = "登录账号长度不能超过100个字符")
    private String identifier; // 可以是用户名、邮箱或手机号

    @NotBlank(message = "密码不能为空")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 判断identifier是否是邮箱格式
     */
    public boolean isEmailLogin() {
        return identifier != null && identifier.contains("@");
    }

    /**
     * 判断identifier是否是手机号格式
     */
    public boolean isPhoneLogin() {
        return identifier != null && identifier.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 判断identifier是否是用户名
     */
    public boolean isUsernameLogin() {
        return !isEmailLogin() && !isPhoneLogin();
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "identifier='" + identifier + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}