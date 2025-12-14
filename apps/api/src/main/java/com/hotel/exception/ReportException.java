package com.hotel.exception;

/**
 * 报表相关异常
 *
 * @author Hotel System
 * @version 1.0
 */
public class ReportException extends RuntimeException {

    private final String errorCode;

    public ReportException(String message) {
        super(message);
        this.errorCode = "REPORT_ERROR";
    }

    public ReportException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "REPORT_ERROR";
    }

    public ReportException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ReportException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}