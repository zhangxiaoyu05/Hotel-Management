package com.hotel.util;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * XSS防护工具类
 *
 * 提供多种XSS防护方法，确保用户生成内容（UGC）安全展示
 */
public class XssProtectionUtil {

    // 基础白名单 - 允许基本的HTML标签
    private static final Safelist BASIC_WHITELIST = Safelist.basic()
            .addAttributes("a", "href", "title", "target")
            .addAttributes("img", "src", "alt", "width", "height", "title")
            .addAttributes("span", "class")
            .addAttributes("div", "class")
            .addAttributes("p", "class")
            .addAttributes("br", "class")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");

    // 严格白名单 - 仅允许文本格式化标签
    private static final Safelist STRICT_WHITELIST = Safelist.none()
            .addTags("b", "i", "em", "strong", "u", "span")
            .addAttributes("span", "class");

    /**
     * 清理用户输入的HTML，防止XSS攻击
     * 使用基础白名单，允许基本格式化
     */
    public static String cleanHtml(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // 使用Jsoup清理HTML
        String cleaned = Jsoup.clean(input, BASIC_WHITELIST);

        // 额外的安全检查
        return postProcessHtml(cleaned);
    }

    /**
     * 严格清理用户输入，仅保留文本格式化
     */
    public static String strictCleanHtml(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        return Jsoup.clean(input, STRICT_WHITELIST);
    }

    /**
     * 转义所有HTML字符，完全不信任用户输入
     */
    public static String escapeHtml(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        return StringEscapeUtils.escapeHtml4(input);
    }

    /**
     * 清理用于显示在属性中的文本
     */
    public static String cleanForAttribute(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // 转义HTML属性中的特殊字符
        return StringEscapeUtils.escapeHtml4(input)
                .replace("'", "&apos;")
                .replace("\"", "&quot;");
    }

    /**
     * 后处理清理后的HTML，移除可能的安全问题
     */
    private static String postProcessHtml(String html) {
        if (html == null) {
            return html;
        }

        // 移除可能的JavaScript事件处理器
        html = html.replaceAll("on\\w+\\s*=\\s*['\"]?[^'\"]*['\"]?", "");

        // 移除可能的data:协议
        html = html.replaceAll("data\\s*:", "");

        // 移除可能的vbscript:协议
        html = html.replaceAll("vbscript\\s*:", "");

        // 移除可能的javascript:协议
        html = html.replaceAll("javascript\\s*:", "");

        return html;
    }

    /**
     * 验证URL是否安全
     */
    public static boolean isSafeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase().trim();

        // 检查危险协议
        if (lowerUrl.startsWith("javascript:") ||
            lowerUrl.startsWith("vbscript:") ||
            lowerUrl.startsWith("data:")) {
            return false;
        }

        // 检查是否是安全的协议
        return lowerUrl.startsWith("http://") ||
               lowerUrl.startsWith("https://") ||
               lowerUrl.startsWith("mailto:");
    }

    /**
     * 清理用户名，仅保留字母数字和基本符号
     */
    public static String cleanUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return username;
        }

        // 保留字母、数字、中文和基本符号
        return username.replaceAll("[^\\w\\u4e00-\\u9fa5@._-]", "");
    }

    /**
     * 检查文本是否包含可疑内容
     */
    public static boolean containsSuspiciousContent(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();

        // 检查常见的XSS模式
        String[] suspiciousPatterns = {
            "<script", "</script", "javascript:", "vbscript:",
            "data:", "onload", "onerror", "onclick", "onmouseover",
            "alert(", "confirm(", "prompt(", "eval(", "expression("
        };

        for (String pattern : suspiciousPatterns) {
            if (lowerText.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 限制文本长度
     */
    public static String truncateText(String text, int maxLength) {
        if (text == null) {
            return null;
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "...";
    }

    /**
     * 清理并截断评价内容
     */
    public static String cleanAndTruncateComment(String comment, int maxLength) {
        if (comment == null) {
            return null;
        }

        // 先清理HTML
        String cleaned = escapeHtml(comment);

        // 再截断长度
        return truncateText(cleaned, maxLength);
    }
}