package com.hotel.service;

import com.hotel.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

/**
 * 用户详情服务实现
 * 为Spring Security提供用户认证信息
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("加载用户详情: {}", username);

        // 根据用户名查找用户
        Optional<com.hotel.entity.User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            logger.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        com.hotel.entity.User user = userOpt.get();

        // 检查用户状态
        if (!"ACTIVE".equals(user.getStatus())) {
            logger.warn("用户已禁用: {}", username);
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 构建用户权限
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());

        // 构建Spring Security用户对象
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // 密码已经加密
                .authorities(Collections.singletonList(authority))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}