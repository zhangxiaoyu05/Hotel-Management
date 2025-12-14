package com.hotel.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.annotation.OperationLog;
import com.hotel.entity.log.OperationLog;
import com.hotel.repository.log.OperationLogRepository;
import com.hotel.util.IpUtil;
import com.hotel.util.PermissionUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 * 自动记录用户操作行为
 */
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 定义切点：所有带有@OperationLog注解的方法
     */
    @Pointcut("@annotation(com.hotel.annotation.OperationLog)")
    public void operationLogPointCut() {}

    /**
     * 环绕通知：记录操作日志
     */
    @Around("operationLogPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            // 执行目标方法
            result = point.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            // 异步记录日志
            saveOperationLog(point, result, exception, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 异步保存操作日志
     */
    @Async
    public void saveOperationLog(ProceedingJoinPoint point, Object result, Exception exception, long time) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();

            // 获取@OperationLog注解信息
            OperationLog annotation = getOperationLogAnnotation(point);
            if (annotation == null || !annotation.save()) {
                return;
            }

            // 构建日志实体
            OperationLog logEntity = new OperationLog();

            // 用户信息
            Long currentUserId = PermissionUtil.getCurrentUserId();
            String currentUsername = PermissionUtil.getCurrentUsername();
            logEntity.setUserId(currentUserId);
            logEntity.setUsername(currentUsername);

            // 操作信息
            logEntity.setOperation(getOperationDescription(annotation, point));
            logEntity.setMethod(getMethodName(point));
            logEntity.setParams(getRequestParams(request, point.getArgs()));
            logEntity.setTime(time);

            // 请求信息
            logEntity.setIp(IpUtil.getClientIp(request));
            logEntity.setUserAgent(request.getHeader("User-Agent"));

            // 结果信息
            if (exception != null) {
                logEntity.setStatus("FAILED");
                logEntity.setErrorMessage(exception.getMessage());
            } else {
                logEntity.setStatus("SUCCESS");
            }

            logEntity.setCreateTime(LocalDateTime.now());

            // 保存到数据库
            operationLogRepository.insert(logEntity);

            log.debug("操作日志记录成功: {} - {}", logEntity.getOperation(), logEntity.getUsername());

        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    /**
     * 获取@OperationLog注解
     */
    private OperationLog getOperationLogAnnotation(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(OperationLog.class);
    }

    /**
     * 获取操作描述
     */
    private String getOperationDescription(OperationLog annotation, ProceedingJoinPoint point) {
        String description = annotation.value();
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }

        // 如果没有描述，使用方法名
        MethodSignature signature = (MethodSignature) point.getSignature();
        return signature.getMethod().getName();
    }

    /**
     * 获取方法名
     */
    private String getMethodName(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        return signature.getDeclaringTypeName() + "." + signature.getName();
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(HttpServletRequest request, Object[] args) {
        try {
            Map<String, Object> params = new HashMap<>();

            // 获取URL参数
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();
                String[] values = request.getParameterValues(name);
                if (values.length == 1) {
                    params.put(name, values[0]);
                } else {
                    params.put(name, Arrays.asList(values));
                }
            }

            // 添加方法参数（仅简单对象，避免循环引用）
            if (args != null && args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg != null && isSimpleType(arg.getClass())) {
                        params.put("arg" + i, arg);
                    }
                }
            }

            // 转换为JSON字符串
            String jsonStr = objectMapper.writeValueAsString(params);

            // 限制长度，避免数据库字段过长
            if (jsonStr.length() > 2000) {
                jsonStr = jsonStr.substring(0, 2000) + "...";
            }

            return jsonStr;

        } catch (Exception e) {
            log.warn("获取请求参数失败", e);
            return "参数获取失败: " + e.getMessage();
        }
    }

    /**
     * 判断是否为简单类型
     */
    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == String.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Double.class ||
               clazz == Float.class ||
               clazz == Boolean.class ||
               clazz == Character.class ||
               Number.class.isAssignableFrom(clazz);
    }
}