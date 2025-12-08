package com.hotel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证入口点
 * 处理未认证的请求，返回401未授权响应
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        logger.error("未授权访问: {} - {}", request.getRequestURI(), authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 构建错误响应
        AuthResponse authResponse = AuthResponse.error("未授权访问，请先登录");

        // 将响应对象转换为JSON并写入响应体
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(authResponse);

        response.getWriter().write(json);
        response.getWriter().flush();
    }
}