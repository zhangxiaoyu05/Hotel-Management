package com.hotel.controller;

import lombok.Data;

/**
 * 基础控制器
 * 提供通用的响应格式
 */
public class BaseController {

    /**
     * 成功响应
     */
    protected <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(data);
        response.setSuccess(true);
        return response;
    }

    /**
     * 成功响应（无数据）
     */
    protected <T> ApiResponse<T> success(String message) {
        return success(null, message);
    }

    /**
     * 失败响应
     */
    protected <T> ApiResponse<T> failed(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(500);
        response.setMessage(message);
        response.setData(null);
        response.setSuccess(false);
        return response;
    }

    /**
     * 失败响应（带错误码）
     */
    protected <T> ApiResponse<T> failed(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(null);
        response.setSuccess(false);
        return response;
    }

    /**
     * 通用API响应格式
     */
    @Data
    public static class ApiResponse<T> {
        private int code;
        private String message;
        private T data;
        private boolean success;
        private long timestamp;

        public ApiResponse() {
            this.timestamp = System.currentTimeMillis();
        }
    }
}