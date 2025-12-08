package com.hotel.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    /**
     * 构造函数
     * @param message 异常消息
     */
    public BusinessException(String message) {
        this(message, "BUSINESS_ERROR", HttpStatus.BAD_REQUEST);
    }

    /**
     * 构造函数
     * @param message 异常消息
     * @param code 错误码
     */
    public BusinessException(String message, String code) {
        this(message, code, HttpStatus.BAD_REQUEST);
    }

    /**
     * 构造函数
     * @param message 异常消息
     * @param code 错误码
     * @param httpStatus HTTP状态码
     */
    public BusinessException(String message, String code, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    /**
     * 构造函数
     * @param message 异常消息
     * @param cause 原因异常
     */
    public BusinessException(String message, Throwable cause) {
        this(message, "BUSINESS_ERROR", HttpStatus.BAD_REQUEST, cause);
    }

    /**
     * 构造函数
     * @param message 异常消息
     * @param code 错误码
     * @param httpStatus HTTP状态码
     * @param cause 原因异常
     */
    public BusinessException(String message, String code, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    // 常用的业务异常静态工厂方法

    /**
     * 参数无效异常
     */
    public static BusinessException invalidParameter(String message) {
        return new BusinessException(message, "INVALID_PARAMETER", HttpStatus.BAD_REQUEST);
    }

    /**
     * 资源不存在异常
     */
    public static BusinessException resourceNotFound(String message) {
        return new BusinessException(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    /**
     * 操作不允许异常
     */
    public static BusinessException operationNotAllowed(String message) {
        return new BusinessException(message, "OPERATION_NOT_ALLOWED", HttpStatus.FORBIDDEN);
    }

    /**
     * 冲突异常
     */
    public static BusinessException conflict(String message) {
        return new BusinessException(message, "CONFLICT", HttpStatus.CONFLICT);
    }

    /**
     * 未授权异常
     */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }
}