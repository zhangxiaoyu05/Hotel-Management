package com.hotel.exception;

/**
 * 导出相关异常
 *
 * @author Hotel System
 * @version 1.0
 */
public class ExportException extends RuntimeException {

    private final String exportType;
    private final String format;

    public ExportException(String message) {
        super(message);
        this.exportType = null;
        this.format = null;
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
        this.exportType = null;
        this.format = null;
    }

    public ExportException(String exportType, String format, String message) {
        super(message);
        this.exportType = exportType;
        this.format = format;
    }

    public ExportException(String exportType, String format, String message, Throwable cause) {
        super(message, cause);
        this.exportType = exportType;
        this.format = format;
    }

    public String getExportType() {
        return exportType;
    }

    public String getFormat() {
        return format;
    }
}