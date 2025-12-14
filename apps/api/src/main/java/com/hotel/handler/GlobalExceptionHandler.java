package com.hotel.handler;

import com.hotel.entity.log.ErrorLog;
import com.hotel.repository.log.ErrorLogRepository;
import com.hotel.util.IpUtil;
import com.hotel.util.PermissionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 全局异常处理器
 * 统一处理异常并记录错误日志
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Autowired
    private ErrorLogRepository errorLogRepository;

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        recordErrorLog(e, "WARN", "BUSINESS");
        return ResponseEntity.ok(ApiResponse.failed(e.getCode(), e.getMessage()));
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("参数验证异常: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String message = "参数验证失败: " + errors.toString();
        recordErrorLog(e, "WARN", "VALIDATION");
        return ResponseEntity.badRequest().body(ApiResponse.failed(400, message));
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(BindException e) {
        log.warn("参数绑定异常: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String message = "参数绑定失败: " + errors.toString();
        recordErrorLog(e, "WARN", "VALIDATION");
        return ResponseEntity.badRequest().body(ApiResponse.failed(400, message));
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("约束违反异常: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        }

        String message = "约束验证失败: " + errors.toString();
        recordErrorLog(e, "WARN", "VALIDATION");
        return ResponseEntity.badRequest().body(ApiResponse.failed(400, message));
    }

    /**
     * 处理访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问拒绝: {}", e.getMessage());
        recordErrorLog(e, "WARN", "AUTHORIZATION");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failed(403, "访问被拒绝，权限不足"));
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Object>> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        recordErrorLog(e, "ERROR", "SYSTEM");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failed(500, "系统内部错误"));
    }

    /**
     * 处理数据库异常
     */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccessException(org.springframework.dao.DataAccessException e) {
        log.error("数据库访问异常", e);
        recordErrorLog(e, "ERROR", "DATABASE");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failed(500, "数据库操作失败"));
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("未知异常", e);
        recordErrorLog(e, "ERROR", "SYSTEM");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failed(500, "系统内部错误，请联系管理员"));
    }

    /**
     * 记录错误日志
     */
    private void recordErrorLog(Exception e, String level, String module) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();

            ErrorLog errorLog = new ErrorLog();
            errorLog.setExceptionType(e.getClass().getSimpleName());
            errorLog.setMessage(e.getMessage());
            errorLog.setStackTrace(getStackTrace(e));
            errorLog.setClassName(e.getStackTrace().length > 0 ? e.getStackTrace()[0].getClassName() : "");
            errorLog.setMethodName(e.getStackTrace().length > 0 ? e.getStackTrace()[0].getMethodName() : "");
            errorLog.setFileName(e.getStackTrace().length > 0 ? e.getStackTrace()[0].getFileName() : "");
            errorLog.setLineNumber(e.getStackTrace().length > 0 ? e.getStackTrace()[0].getLineNumber() : 0);
            errorLog.setUrl(request.getRequestURI());
            errorLog.setParams(getRequestParams(request));
            errorLog.setIp(IpUtil.getClientIp(request));
            errorLog.setUserAgent(request.getHeader("User-Agent"));
            errorLog.setUserId(PermissionUtil.getCurrentUserId());
            errorLog.setUsername(PermissionUtil.getCurrentUsername());
            errorLog.setLevel(level);
            errorLog.setModule(module);
            errorLog.setCreateTime(LocalDateTime.now());

            // 异步保存错误日志
            saveErrorLogAsync(errorLog);

        } catch (Exception ex) {
            log.error("记录错误日志失败", ex);
        }
    }

    /**
     * 异步保存错误日志
     */
    private void saveErrorLogAsync(ErrorLog errorLog) {
        try {
            errorLogRepository.insert(errorLog);
        } catch (Exception e) {
            log.error("保存错误日志到数据库失败", e);
        }
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            String stackTrace = sw.toString();

            // 限制长度，避免数据库字段过长
            if (stackTrace.length() > 5000) {
                stackTrace = stackTrace.substring(0, 5000) + "...";
            }

            return stackTrace;
        } catch (Exception ex) {
            return "获取堆栈信息失败: " + ex.getMessage();
        }
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(HttpServletRequest request) {
        try {
            Map<String, String[]> paramMap = request.getParameterMap();
            StringBuilder params = new StringBuilder();

            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                if (params.length() > 0) {
                    params.append("&");
                }
                params.append(entry.getKey()).append("=");
                String[] values = entry.getValue();
                if (values.length == 1) {
                    params.append(values[0]);
                } else {
                    params.append("[");
                    for (int i = 0; i < values.length; i++) {
                        if (i > 0) {
                            params.append(",");
                        }
                        params.append(values[i]);
                    }
                    params.append("]");
                }
            }

            // 限制长度
            if (params.length() > 1000) {
                params = new StringBuilder(params.substring(0, 1000) + "...");
            }

            return params.toString();

        } catch (Exception e) {
            return "获取请求参数失败: " + e.getMessage();
        }
    }

    /**
     * 自定义业务异常
     */
    public static class BusinessException extends RuntimeException {
        private int code;

        public BusinessException(int code, String message) {
            super(message);
            this.code = code;
        }

        public BusinessException(String message) {
            this(500, message);
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * API响应包装类
     */
    public static class ApiResponse<T> {
        private int code;
        private String message;
        private T data;

        public ApiResponse() {}

        public ApiResponse(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.code = 200;
            response.message = message;
            response.data = data;
            return response;
        }

        public static <T> ApiResponse<T> failed(int code, String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.code = code;
            response.message = message;
            return response;
        }

        // getters and setters
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }
}