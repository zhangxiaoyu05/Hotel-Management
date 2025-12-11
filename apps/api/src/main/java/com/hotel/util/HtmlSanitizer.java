package com.hotel.util;

import org.owasp.encoder.Encode;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * HTML内容清理工具类
 * 用于防止XSS攻击，清理用户输入的HTML内容
 */
public class HtmlSanitizer {

    // 允许的HTML标签（根据业务需求调整）
    private static final Safelist ALLOWED_TAGS = Safelist.none()
            .addTags("b", "i", "u", "em", "strong", "p", "br")
            .addAttributes("p", "style")
            .addAttributes("span", "style");

    // 允许的CSS属性
    private static final String[] ALLOWED_CSS_PROPERTIES = {
        "color", "font-weight", "font-style", "text-decoration"
    };

    /**
     * 清理用户输入，防止XSS攻击
     * @param input 用户输入的内容
     * @return 清理后的安全内容
     */
    public static String sanitize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // 1. 使用JSoup清理HTML标签（保留基本格式）
        String cleaned = Jsoup.clean(input, ALLOWED_TAGS);

        // 2. 对清理后的内容进行HTML编码
        // 注意：这里不进行编码，因为JSoup已经处理了危险内容
        // 编码会破坏正常的文本显示

        // 3. 移除可能的JavaScript事件处理器
        cleaned = removeEventHandlers(cleaned);

        // 4. 移除可能的协议（如javascript:）
        cleaned = removeDangerousProtocols(cleaned);

        return cleaned.trim();
    }

    /**
     * 完全转义HTML内容（用于显示纯文本）
     * @param input 用户输入的内容
     * @return 转义后的内容
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return Encode.forHtml(input);
    }

    /**
     * 转义用于JavaScript的内容
     * @param input 用户输入的内容
     * @return 转义后的内容
     */
    public static String escapeJavaScript(String input) {
        if (input == null) {
            return null;
        }
        return Encode.forJavaScript(input);
    }

    /**
     * 转义用于CSS的内容
     * @param input 用户输入的内容
     * @return 转义后的内容
     */
    public static String escapeCss(String input) {
        if (input == null) {
            return null;
        }
        return Encode.forCssString(input);
    }

    /**
     * 移除事件处理器
     */
    private static String removeEventHandlers(String input) {
        if (input == null) {
            return null;
        }

        // 移除on*属性
        String cleaned = input.replaceAll("(?i)\\bon\\w+\\s*=\\s*['\"]?[^'\"\\s>]*['\"]?", "");

        return cleaned;
    }

    /**
     * 移除危险协议
     */
    private static String removeDangerousProtocols(String input) {
        if (input == null) {
            return null;
        }

        // 移除javascript:协议
        String cleaned = input.replaceAll("(?i)javascript\\s*:", "");

        // 移除data:协议（可能包含base64编码的脚本）
        cleaned = cleaned.replaceAll("(?i)data\\s*:", "");

        // 移除vbscript:协议
        cleaned = cleaned.replaceAll("(?i)vbscript\\s*:", "");

        return cleaned;
    }

    /**
     * 验证CSS属性是否安全
     * @param cssProperty CSS属性名
     * @return 是否安全
     */
    public static boolean isSafeCssProperty(String cssProperty) {
        if (cssProperty == null) {
            return false;
        }

        for (String allowed : ALLOWED_CSS_PROPERTIES) {
            if (allowed.equalsIgnoreCase(cssProperty)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 清理URL，防止JavaScript注入
     * @param url 输入的URL
     * @return 安全的URL
     */
    public static String sanitizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }

        // 转义URL
        String sanitized = Encode.forUri(url);

        // 确保不是危险的协议
        String lowerUrl = sanitized.toLowerCase();
        if (lowerUrl.startsWith("javascript:") ||
            lowerUrl.startsWith("data:") ||
            lowerUrl.startsWith("vbscript:")) {
            return "#"; // 返回安全的默认值
        }

        return sanitized;
    }
}