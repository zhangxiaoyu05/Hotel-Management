package com.hotel.listener;

import com.hotel.entity.log.LoginLog;
import com.hotel.repository.log.LoginLogRepository;
import com.hotel.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 登录日志事件监听器
 * 监听登录/退出事件并记录日志
 */
@Component
@Slf4j
public class LoginLogListener {

    @Autowired
    private LoginLogRepository loginLogRepository;

    /**
     * 监听登录事件
     */
    @EventListener
    @Async
    public void handleLoginEvent(LoginEvent event) {
        try {
            log.debug("处理登录事件: {}", event);

            LoginLog loginLog = new LoginLog();
            loginLog.setUsername(event.getUsername());
            loginLog.setLoginType("LOGIN");
            loginLog.setIp(event.getIp());
            loginLog.setLocation(getLocationFromIp(event.getIp()));
            loginLog.setBrowser(parseBrowser(event.getUserAgent()));
            loginLog.setOs(parseOs(event.getUserAgent()));
            loginLog.setStatus(event.isSuccess() ? "SUCCESS" : "FAILED");
            loginLog.setMessage(event.getMessage());
            loginLog.setUserAgent(event.getUserAgent());
            loginLog.setSessionId(event.getSessionId());
            loginLog.setCreateTime(LocalDateTime.now());

            loginLogRepository.insert(loginLog);
            log.debug("登录日志记录成功: {} - {}", event.getUsername(), event.getStatus());

        } catch (Exception e) {
            log.error("保存登录日志失败", e);
        }
    }

    /**
     * 监听退出事件
     */
    @EventListener
    @Async
    public void handleLogoutEvent(LogoutEvent event) {
        try {
            log.debug("处理退出事件: {}", event);

            LoginLog loginLog = new LoginLog();
            loginLog.setUsername(event.getUsername());
            loginLog.setLoginType("LOGOUT");
            loginLog.setIp(event.getIp());
            loginLog.setLocation(getLocationFromIp(event.getIp()));
            loginLog.setBrowser(parseBrowser(event.getUserAgent()));
            loginLog.setOs(parseOs(event.getUserAgent()));
            loginLog.setStatus("SUCCESS");
            loginLog.setMessage("用户退出登录");
            loginLog.setUserAgent(event.getUserAgent());
            loginLog.setSessionId(event.getSessionId());
            loginLog.setCreateTime(LocalDateTime.now());

            loginLogRepository.insert(loginLog);
            log.debug("退出日志记录成功: {}", event.getUsername());

        } catch (Exception e) {
            log.error("保存退出日志失败", e);
        }
    }

    /**
     * 根据IP地址获取地理位置（简单实现，实际项目可以调用第三方API）
     */
    private String getLocationFromIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "未知";
        }

        // 内网IP
        if (IpUtil.isInternalIp(ip)) {
            return "内网";
        }

        // 这里可以集成第三方IP定位服务，如：
        // - 淘宝IP地址库API
        // - 新浪IP地址库API
        // - 百度地图IP定位API等
        return "未知地区";
    }

    /**
     * 解析浏览器信息
     */
    private String parseBrowser(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "未知";
        }

        try {
            // Chrome
            if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) {
                Matcher matcher = Pattern.compile("Chrome/(\\d+\\.\\d+)").matcher(userAgent);
                if (matcher.find()) {
                    return "Chrome " + matcher.group(1);
                }
                return "Chrome";
            }

            // Firefox
            if (userAgent.contains("Firefox")) {
                Matcher matcher = Pattern.compile("Firefox/(\\d+\\.\\d+)").matcher(userAgent);
                if (matcher.find()) {
                    return "Firefox " + matcher.group(1);
                }
                return "Firefox";
            }

            // Safari
            if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
                Matcher matcher = Pattern.compile("Version/(\\d+\\.\\d+)").matcher(userAgent);
                if (matcher.find()) {
                    return "Safari " + matcher.group(1);
                }
                return "Safari";
            }

            // Edge
            if (userAgent.contains("Edg")) {
                Matcher matcher = Pattern.compile("Edg/(\\d+\\.\\d+)").matcher(userAgent);
                if (matcher.find()) {
                    return "Edge " + matcher.group(1);
                }
                return "Edge";
            }

            // IE
            if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
                return "IE";
            }

            return "未知浏览器";

        } catch (Exception e) {
            log.warn("解析浏览器信息失败: {}", userAgent, e);
            return "未知";
        }
    }

    /**
     * 解析操作系统信息
     */
    private String parseOs(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "未知";
        }

        try {
            // Windows
            if (userAgent.contains("Windows")) {
                if (userAgent.contains("Windows NT 10.0")) {
                    return "Windows 10/11";
                } else if (userAgent.contains("Windows NT 6.3")) {
                    return "Windows 8.1";
                } else if (userAgent.contains("Windows NT 6.2")) {
                    return "Windows 8";
                } else if (userAgent.contains("Windows NT 6.1")) {
                    return "Windows 7";
                }
                return "Windows";
            }

            // macOS
            if (userAgent.contains("Mac")) {
                if (userAgent.contains("Mac OS X")) {
                    Matcher matcher = Pattern.compile("Mac OS X ([0-9_]+)").matcher(userAgent);
                    if (matcher.find()) {
                        return "macOS " + matcher.group(1).replace("_", ".");
                    }
                }
                return "macOS";
            }

            // Linux
            if (userAgent.contains("Linux")) {
                if (userAgent.contains("Ubuntu")) {
                    return "Ubuntu";
                }
                return "Linux";
            }

            // Android
            if (userAgent.contains("Android")) {
                Matcher matcher = Pattern.compile("Android ([0-9\\.]+)").matcher(userAgent);
                if (matcher.find()) {
                    return "Android " + matcher.group(1);
                }
                return "Android";
            }

            // iOS
            if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
                if (userAgent.contains("iPhone")) {
                    return "iPhone";
                } else if (userAgent.contains("iPad")) {
                    return "iPad";
                }
                return "iOS";
            }

            return "未知系统";

        } catch (Exception e) {
            log.warn("解析操作系统信息失败: {}", userAgent, e);
            return "未知";
        }
    }

    /**
     * 登录事件
     */
    public static class LoginEvent {
        private String username;
        private boolean success;
        private String message;
        private String ip;
        private String userAgent;
        private String sessionId;
        private String status;

        // getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /**
     * 退出事件
     */
    public static class LogoutEvent {
        private String username;
        private String ip;
        private String userAgent;
        private String sessionId;

        // getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
}