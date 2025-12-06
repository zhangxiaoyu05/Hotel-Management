package com.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.dto.ChangePasswordRequest;
import com.hotel.dto.CreateUserRequest;
import com.hotel.dto.LoginRequest;
import com.hotel.dto.UpdateProfileRequest;
import com.hotel.dto.AuthResponse;
import com.hotel.entity.User;
import com.hotel.repository.UserRepository;
import com.hotel.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    public AuthResponse.Data registerUser(CreateUserRequest request) {
        logger.info("开始用户注册: {}", request.getUsername());

        try {
            // 检查用户名是否已存在
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("用户名已存在");
            }

            // 检查邮箱是否已存在
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("邮箱已被注册");
            }

            // 检查手机号是否已存在
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new IllegalArgumentException("手机号已被注册");
            }

            // 创建新用户
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole() != null ? request.getRole() : "USER");
            user.setStatus("ACTIVE");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            // 保存用户
            userRepository.insert(user);
            logger.info("用户注册成功: {}", user.getUsername());

            // 构建响应数据
            AuthResponse.Data data = new AuthResponse.Data();
            AuthResponse.User userResponse = new AuthResponse.User();
            userResponse.setId(user.getId());
            userResponse.setUsername(user.getUsername());
            userResponse.setEmail(user.getEmail());
            userResponse.setPhone(user.getPhone());
            userResponse.setRole(user.getRole());
            userResponse.setStatus(user.getStatus());

            data.setUser(userResponse);
            // 注册时不生成token，需要登录
            data.setToken(null);

            return data;

        } catch (IllegalArgumentException e) {
            logger.warn("用户注册失败 - 参数错误: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("用户注册失败 - 系统错误: {}", e.getMessage(), e);
            throw new RuntimeException("注册失败，请稍后重试");
        }
    }

    /**
     * 根据用户名查找用户
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据邮箱查找用户
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 根据手机号查找用户
     */
    @Transactional(readOnly = true)
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    /**
     * 根据用户标识查找用户（用户名/邮箱/手机号）
     */
    @Transactional(readOnly = true)
    public Optional<User> findByIdentifier(String identifier) {
        return userRepository.findByIdentifier(identifier);
    }

    /**
     * 根据ID查找用户
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userRepository.selectById(id));
    }

    /**
     * 检查用户名是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 检查手机号是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    /**
     * 更新用户信息
     */
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.updateById(user);
        return user;
    }

    /**
     * 激活/停用用户
     */
    public User updateUserStatus(Long userId, String status) {
        Optional<User> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("用户不存在");
        }

        User user = userOpt.get();
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.updateById(user);

        return user;
    }

    /**
     * 用户登录
     */
    @Transactional(readOnly = true)
    public AuthResponse.Data login(LoginRequest loginRequest) {
        logger.info("用户登录请求: {}", loginRequest.getIdentifier());

        // 根据登录标识查找用户
        Optional<User> userOpt = userRepository.findByIdentifier(loginRequest.getIdentifier());

        if (userOpt.isEmpty()) {
            logger.warn("登录失败 - 用户不存在: {}", loginRequest.getIdentifier());
            throw new IllegalArgumentException("用户名、邮箱或手机号不存在");
        }

        User user = userOpt.get();

        // 检查用户状态
        if (!"ACTIVE".equals(user.getStatus())) {
            logger.warn("登录失败 - 用户已禁用: {}", user.getUsername());
            throw new IllegalArgumentException("账户已被禁用，请联系管理员");
        }

        // 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            logger.warn("登录失败 - 密码错误: {}", user.getUsername());
            throw new IllegalArgumentException("用户名或密码错误");
        }

        logger.info("用户登录成功: {}", user.getUsername());

        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId());

        // 构建响应数据
        AuthResponse.Data data = new AuthResponse.Data();
        data.setToken(token);
        data.setAccessToken(token);
        data.setRefreshToken(refreshToken);
        data.setExpiresIn(86400L); // 24小时

        AuthResponse.User userResponse = new AuthResponse.User();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setPhone(user.getPhone());
        userResponse.setRole(user.getRole());
        userResponse.setStatus(user.getStatus());

        data.setUser(userResponse);

        // 更新最后登录时间
        updateUserLastLogin(user.getId());

        return data;
    }

    /**
     * 更新用户最后登录时间
     */
    private void updateUserLastLogin(Long userId) {
        try {
            // 使用更高效的更新方式，避免先查询再更新
            User user = new User();
            user.setId(userId);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.updateById(user);
        } catch (Exception e) {
            logger.warn("更新用户最后登录时间失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 根据用户名获取用户信息
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("用户不存在");
        }
        return userOpt.get();
    }

    /**
     * 更新用户资料
     */
    public User updateUserProfile(String username, UpdateProfileRequest updateRequest) {
        logger.info("开始更新用户资料: {}", username);

        try {
            // 获取当前用户
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("用户不存在");
            }

            User user = userOpt.get();

            // 检查邮箱是否已被其他用户使用
            if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(updateRequest.getEmail())) {
                    throw new IllegalArgumentException("邮箱已被其他用户使用");
                }
            }

            // 检查手机号是否已被其他用户使用
            if (updateRequest.getPhone() != null && !updateRequest.getPhone().equals(user.getPhone())) {
                if (userRepository.existsByPhone(updateRequest.getPhone())) {
                    throw new IllegalArgumentException("手机号已被其他用户使用");
                }
            }

            // 更新用户信息
            if (updateRequest.getNickname() != null) {
                user.setNickname(updateRequest.getNickname());
            }
            if (updateRequest.getRealName() != null) {
                user.setRealName(updateRequest.getRealName());
            }
            if (updateRequest.getGender() != null) {
                user.setGender(updateRequest.getGender());
            }
            if (updateRequest.getBirthDate() != null) {
                user.setBirthDate(updateRequest.getBirthDate());
            }
            if (updateRequest.getAvatar() != null) {
                user.setAvatar(updateRequest.getAvatar());
            }
            if (updateRequest.getEmail() != null) {
                user.setEmail(updateRequest.getEmail());
            }
            if (updateRequest.getPhone() != null) {
                user.setPhone(updateRequest.getPhone());
            }

            user.setUpdatedAt(LocalDateTime.now());

            // 保存更新
            userRepository.updateById(user);

            logger.info("用户资料更新成功: {}", username);
            return user;

        } catch (IllegalArgumentException e) {
            logger.warn("用户资料更新失败 - 参数错误: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("用户资料更新失败 - 系统错误: {}", e.getMessage(), e);
            throw new RuntimeException("更新用户资料失败，请稍后重试");
        }
    }

    /**
     * 修改密码
     */
    public void changePassword(String username, ChangePasswordRequest changePasswordRequest) {
        logger.info("开始修改用户密码: {}", username);

        try {
            // 获取当前用户
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("用户不存在");
            }

            User user = userOpt.get();

            // 验证当前密码
            if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("当前密码不正确");
            }

            // 检查新密码是否与当前密码相同
            if (passwordEncoder.matches(changePasswordRequest.getNewPassword(), user.getPassword())) {
                throw new IllegalArgumentException("新密码不能与当前密码相同");
            }

            // 更新密码
            user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.updateById(user);

            logger.info("用户密码修改成功: {}", username);

        } catch (IllegalArgumentException e) {
            logger.warn("用户密码修改失败 - 参数错误: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("用户密码修改失败 - 系统错误: {}", e.getMessage(), e);
            throw new RuntimeException("修改密码失败，请稍后重试");
        }
    }
}