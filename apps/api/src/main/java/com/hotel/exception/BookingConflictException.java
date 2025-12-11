package com.hotel.exception;

import lombok.Getter;

/**
 * 预订冲突相关异常基类
 *
 * @author System
 * @since 1.0
 */
@Getter
public class BookingConflictException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public BookingConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }

    public BookingConflictException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = null;
    }

    public BookingConflictException(String errorCode, String message, Object[] args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public BookingConflictException(String errorCode, String message, Object[] args, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * 冲突检测异常
     */
    public static class ConflictDetectionException extends BookingConflictException {
        public ConflictDetectionException(String errorCode, String message) {
            super(errorCode, message);
        }

        public ConflictDetectionException(String errorCode, String message, Throwable cause) {
            super(errorCode, message, cause);
        }
    }

    /**
     * 等待列表异常
     */
    public static class WaitingListException extends BookingConflictException {
        public WaitingListException(String errorCode, String message) {
            super(errorCode, message);
        }

        public WaitingListException(String errorCode, String message, Object[] args) {
            super(errorCode, message, args);
        }

        public WaitingListException(String errorCode, String message, Throwable cause) {
            super(errorCode, message, cause);
        }
    }

    /**
     * 并发冲突异常
     */
    public static class ConcurrentConflictException extends BookingConflictException {
        public ConcurrentConflictException(String errorCode, String message) {
            super(errorCode, message);
        }

        public ConcurrentConflictException(String errorCode, String message, Object[] args) {
            super(errorCode, message, args);
        }
    }

    /**
     * 房间可用性异常
     */
    public static class RoomAvailabilityException extends BookingConflictException {
        public RoomAvailabilityException(String errorCode, String message) {
            super(errorCode, message);
        }

        public RoomAvailabilityException(String errorCode, String message, Throwable cause) {
            super(errorCode, message, cause);
        }
    }

    /**
     * 通知发送异常
     */
    public static class NotificationException extends BookingConflictException {
        public NotificationException(String errorCode, String message) {
            super(errorCode, message);
        }

        public NotificationException(String errorCode, String message, Throwable cause) {
            super(errorCode, message, cause);
        }
    }

    /**
     * 数据验证异常
     */
    public static class ValidationException extends BookingConflictException {
        public ValidationException(String errorCode, String message) {
            super(errorCode, message);
        }

        public ValidationException(String errorCode, String message, Object[] args) {
            super(errorCode, message, args);
        }
    }
}