package com.hotel.security;

import com.hotel.entity.User;
import com.hotel.enums.Role;
import com.hotel.enums.Permission;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义用户详情类
 * 包含用户信息和权限信息
 */
@Data
@EqualsAndHashCode
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String phone;
    private final String password;
    private final Role role;
    private final String status;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.password = user.getPassword();
        this.role = Role.fromCode(user.getRole());
        this.status = user.getStatus();
        this.authorities = buildAuthorities(user.getRole());
    }

    /**
     * 根据用户角色构建权限列表
     */
    private Collection<? extends GrantedAuthority> buildAuthorities(String roleCode) {
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_" + roleCode)
        );

        // 根据角色添加具体权限
        if (Role.ADMIN.getCode().equals(roleCode)) {
            // 管理员拥有所有权限
            authorities.addAll(Arrays.stream(Permission.values())
                    .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
                    .collect(Collectors.toList()));
        } else if (Role.USER.getCode().equals(roleCode)) {
            // 普通用户只有基本权限
            authorities.addAll(Arrays.asList(
                    new SimpleGrantedAuthority(Permission.USER_READ.getCode()),
                    new SimpleGrantedAuthority(Permission.BOOKING_READ.getCode()),
                    new SimpleGrantedAuthority(Permission.BOOKING_WRITE.getCode()),
                    new SimpleGrantedAuthority(Permission.REVIEW_READ.getCode()),
                    new SimpleGrantedAuthority(Permission.REVIEW_WRITE.getCode()),
                    new SimpleGrantedAuthority(Permission.ROOM_READ.getCode())
            ));
        }

        return authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }

    /**
     * 检查用户是否有指定权限
     */
    public boolean hasPermission(Permission permission) {
        return authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals(permission.getCode()));
    }

    /**
     * 检查用户是否有指定角色
     */
    public boolean hasRole(Role role) {
        return this.role.equals(role);
    }

    /**
     * 检查用户是否为管理员
     */
    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }
}