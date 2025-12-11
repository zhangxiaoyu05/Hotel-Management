package com.hotel.exception;

import org.springframework.http.HttpStatus;

/**
 * 资源未找到异常
 * 当请求的资源不存在时抛出
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * 构造函数
     * @param message 异常消息
     */
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    /**
     * 构造函数
     * @param resourceName 资源名称
     * @param resourceId 资源ID
     */
    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(String.format("%s 不存在，ID: %s", resourceName, resourceId),
              "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    /**
     * 房间不存在异常
     */
    public static ResourceNotFoundException roomNotFound(Long roomId) {
        return new ResourceNotFoundException("房间", roomId);
    }

    /**
     * 酒店不存在异常
     */
    public static ResourceNotFoundException hotelNotFound(Long hotelId) {
        return new ResourceNotFoundException("酒店", hotelId);
    }

    /**
     * 用户不存在异常
     */
    public static ResourceNotFoundException userNotFound(Long userId) {
        return new ResourceNotFoundException("用户", userId);
    }

    /**
     * 订单不存在异常
     */
    public static ResourceNotFoundException orderNotFound(Long orderId) {
        return new ResourceNotFoundException("订单", orderId);
    }

    /**
     * 房间类型不存在异常
     */
    public static ResourceNotFoundException roomTypeNotFound(Long roomTypeId) {
        return new ResourceNotFoundException("房间类型", roomTypeId);
    }
}