package com.hotel.security;

import com.hotel.service.UserDetailsServiceImpl;
import com.hotel.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT请求过滤器
 * 验证JWT令牌并设置认证信息
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // 检查Authorization头是否包含Bearer token
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(jwt);
            } catch (Exception e) {
                logger.warn("JWT令牌解析失败: {}", e.getMessage());
                // 继续执行过滤器链，让认证入口点处理错误
                chain.doFilter(request, response);
                return;
            }
        }

        // 如果从token中提取了用户名且当前没有认证信息
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 验证令牌是否有效
                if (jwtUtil.validateToken(jwt, username)) {
                    // 加载用户详情
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                    // 创建认证令牌
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 设置认证信息到安全上下文
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    logger.debug("用户 {} 认证成功", username);
                } else {
                    logger.warn("JWT令牌验证失败: {}", username);
                }
            } catch (Exception e) {
                logger.error("用户认证过程中发生错误: {}", e.getMessage());
                // 继续执行过滤器链，让认证入口点处理错误
            }
        }

        // 继续执行过滤器链
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // 跳过不需要JWT验证的路径
        return path.startsWith("/v1/auth/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/actuator") ||
               path.equals("/error") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/");
    }
}