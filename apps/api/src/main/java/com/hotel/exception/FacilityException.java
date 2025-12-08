package com.hotel.exception;

/**
 * 设施管理相关的异常类
 */
public class FacilityException extends RuntimeException {

    private final String errorCode;

    public FacilityException(String message) {
        super(message);
        this.errorCode = "FACILITY_ERROR";
    }

    public FacilityException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "FACILITY_ERROR";
    }

    public FacilityException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public FacilityException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 设施未找到异常
     */
    public static class FacilityNotFoundException extends FacilityException {
        public FacilityNotFoundException(String message) {
            super("FACILITY_NOT_FOUND", message);
        }
    }

    /**
     * 设施分类未找到异常
     */
    public static class CategoryNotFoundException extends FacilityException {
        public CategoryNotFoundException(String message) {
            super("CATEGORY_NOT_FOUND", message);
        }
    }

    /**
     * 设施名称已存在异常
     */
    public static class FacilityNameExistsException extends FacilityException {
        public FacilityNameExistsException(String message) {
            super("FACILITY_NAME_EXISTS", message);
        }
    }

    /**
     * 分类名称已存在异常
     */
    public static class CategoryNameExistsException extends FacilityException {
        public CategoryNameExistsException(String message) {
            super("CATEGORY_NAME_EXISTS", message);
        }
    }

    /**
     * 无效的设施状态异常
     */
    public static class InvalidFacilityStatusException extends FacilityException {
        public InvalidFacilityStatusException(String message) {
            super("INVALID_FACILITY_STATUS", message);
        }
    }

    /**
     * 设施操作异常
     */
    public static class FacilityOperationException extends FacilityException {
        public FacilityOperationException(String message) {
            super("FACILITY_OPERATION_ERROR", message);
        }

        public FacilityOperationException(String message, Throwable cause) {
            super("FACILITY_OPERATION_ERROR", message, cause);
        }
    }
}