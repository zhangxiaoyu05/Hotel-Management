package com.hotel.controller.auth;

import com.hotel.dto.AuthResponse;
import com.hotel.dto.CreateUserRequest;
import com.hotel.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "认证管理", description = "用户认证相关接口")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户账户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "注册成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "用户已存在",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "服务器内部错误",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody CreateUserRequest request) {

        logger.info("收到用户注册请求: {}", request.getUsername());

        try {
            // 验证密码确认（如果前端没有做验证的话）
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                AuthResponse response = AuthResponse.error("密码不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 调用服务层进行用户注册
            AuthResponse.Data data = userService.registerUser(request);

            // 构建成功响应
            AuthResponse response = AuthResponse.success("注册成功", data);
            logger.info("用户注册成功: {}", request.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("用户注册失败 - 参数错误: {}", e.getMessage());
            AuthResponse response = AuthResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("用户注册失败 - 系统错误: {}", e.getMessage(), e);
            AuthResponse response = AuthResponse.error("注册失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/validate/username")
    @Operation(summary = "验证用户名", description = "检查用户名是否可用")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "验证成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    public ResponseEntity<AuthResponse> validateUsername(
            @Parameter(description = "用户名", required = true)
            @RequestParam String username) {

        try {
            if (username == null || username.trim().isEmpty()) {
                AuthResponse response = AuthResponse.error("用户名不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            if (username.length() < 3 || username.length() > 50) {
                AuthResponse response = AuthResponse.error("用户名长度必须在3到50个字符之间");
                return ResponseEntity.badRequest().body(response);
            }

            boolean exists = userService.existsByUsername(username);
            AuthResponse response = exists ?
                    AuthResponse.error("用户名已存在") :
                    AuthResponse.success("用户名可用");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("验证用户名失败: {}", e.getMessage(), e);
            AuthResponse response = AuthResponse.error("验证失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/validate/email")
    @Operation(summary = "验证邮箱", description = "检查邮箱是否可用")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "验证成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    public ResponseEntity<AuthResponse> validateEmail(
            @Parameter(description = "邮箱", required = true)
            @RequestParam String email) {

        try {
            if (email == null || email.trim().isEmpty()) {
                AuthResponse response = AuthResponse.error("邮箱不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 简单的邮箱格式验证
            if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                AuthResponse response = AuthResponse.error("邮箱格式不正确");
                return ResponseEntity.badRequest().body(response);
            }

            boolean exists = userService.existsByEmail(email);
            AuthResponse response = exists ?
                    AuthResponse.error("邮箱已被注册") :
                    AuthResponse.success("邮箱可用");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("验证邮箱失败: {}", e.getMessage(), e);
            AuthResponse response = AuthResponse.error("验证失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/validate/phone")
    @Operation(summary = "验证手机号", description = "检查手机号是否可用")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "验证成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    public ResponseEntity<AuthResponse> validatePhone(
            @Parameter(description = "手机号", required = true)
            @RequestParam String phone) {

        try {
            if (phone == null || phone.trim().isEmpty()) {
                AuthResponse response = AuthResponse.error("手机号不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 中国手机号格式验证
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                AuthResponse response = AuthResponse.error("手机号格式不正确");
                return ResponseEntity.badRequest().body(response);
            }

            boolean exists = userService.existsByPhone(phone);
            AuthResponse response = exists ?
                    AuthResponse.error("手机号已被注册") :
                    AuthResponse.success("手机号可用");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("验证手机号失败: {}", e.getMessage(), e);
            AuthResponse response = AuthResponse.error("验证失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}