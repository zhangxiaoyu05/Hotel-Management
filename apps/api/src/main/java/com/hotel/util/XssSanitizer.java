package com.hotel.util;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.util.regex.Pattern;

/**
 * XSS 防护工具类
 * 用于清理用户输入，防止跨站脚本攻击
 */
public class XssSanitizer {

    // XSS 攻击常见模式
    private static final Pattern[] XSS_PATTERNS = {
            // 脚本标签
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),

            // JavaScript 事件
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("onerror(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("onclick(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("onmouseover(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("onfocus(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("onblur(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),

            // 其他危险标签
            Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("<embed[^>]*>.*?</embed>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("<applet[^>]*>.*?</applet>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("<meta[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<link[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<style[^>]*>.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),

            // 表达式
            Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),

            // URL 中的 javascript
            Pattern.compile("url\\(.*?javascript:.*?\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
    };

    // HTML 实体编码
    private static final String[][] HTML_ENTITIES = {
            {"<", "&lt;"},
            {">", "&gt;"},
            {"\"", "&quot;"},
            {"'", "&#x27;"},
            {"/", "&#x2F;"},
            {"&", "&amp;"}
    };

    /**
     * 清理字符串中的 XSS 攻击代码
     * @param input 用户输入字符串
     * @return 清理后的安全字符串
     */
    public static String sanitize(String input) {
        if (StringUtils.isBlank(input)) {
            return input;
        }

        String cleanInput = input;

        // 1. 使用正则表达式移除危险的脚本和标签
        for (Pattern pattern : XSS_PATTERNS) {
            cleanInput = pattern.matcher(cleanInput).replaceAll("");
        }

        // 2. 使用 JSoup 进行 HTML 清理
        cleanInput = Jsoup.clean(cleanInput, Safelist.none());

        // 3. HTML 实体编码
        cleanInput = htmlEncode(cleanInput);

        // 4. 移除潜在的 Unicode 编码的脚本
        cleanInput = removeUnicodeScripts(cleanInput);

        return cleanInput.trim();
    }

    /**
     * 允许基本 HTML 标签的清理（用于富文本内容）
     * @param input 用户输入字符串
     * @return 清理后的安全字符串
     */
    public static String sanitizeWithBasicHtml(String input) {
        if (StringUtils.isBlank(input)) {
            return input;
        }

        String cleanInput = input;

        // 1. 移除危险脚本
        for (Pattern pattern : XSS_PATTERNS) {
            cleanInput = pattern.matcher(cleanInput).replaceAll("");
        }

        // 2. 使用 JSoup 允许基本 HTML 标签
        Safelist safelist = Safelist.basic()
                .addTags("p", "br", "strong", "em", "u", "ul", "ol", "li")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "http", "https", "mailto");

        cleanInput = Jsoup.clean(cleanInput, safelist);

        // 3. 仍然对一些特殊字符进行编码
        cleanInput = htmlEncodeSpecialChars(cleanInput);

        return cleanInput.trim();
    }

    /**
     * HTML 实体编码
     */
    private static String htmlEncode(String input) {
        if (StringUtils.isBlank(input)) {
            return input;
        }

        String encoded = input;
        for (String[] entity : HTML_ENTITIES) {
            encoded = encoded.replace(entity[0], entity[1]);
        }
        return encoded;
    }

    /**
     * 编码特殊字符
     */
    private static String htmlEncodeSpecialChars(String input) {
        if (StringUtils.isBlank(input)) {
            return input;
        }

        return input.replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    /**
     * 移除 Unicode 编码的脚本
     */
    private static String removeUnicodeScripts(String input) {
        if (StringUtils.isBlank(input)) {
            return input;
        }

        // 移除常见的 Unicode 编码的 javascript
        return input.replaceAll("(?i)\\u006a\\u0061\\u0076\\u0061\\u0073\\u0063\\u0072\\u0069\\u0070\\u0074", "")
                   .replaceAll("(?i)\\u006a\\u0061\\u0076\\u0061\\u0073\\u0063\\u0072\\u0069\\u0070\\u0074\\u003a", "");
    }

    /**
     * 验证输入是否包含 XSS 攻击
     * @param input 用户输入字符串
     * @return true 如果包含 XSS 攻击代码
     */
    public static boolean containsXss(String input) {
        if (StringUtils.isBlank(input)) {
            return false;
        }

        String lowerInput = input.toLowerCase();

        // 检查是否包含危险关键词
        return lowerInput.contains("<script") ||
               lowerInput.contains("javascript:") ||
               lowerInput.contains("vbscript:") ||
               lowerInput.contains("onload=") ||
               lowerInput.contains("onerror=") ||
               lowerInput.contains("onclick=") ||
               lowerInput.contains("onmouseover=") ||
               lowerInput.contains("<iframe") ||
               lowerInput.contains("<object") ||
               lowerInput.contains("<embed") ||
               lowerInput.contains("eval(") ||
               lowerInput.contains("expression(");
    }

    /**
     * 清理 URL 参数
     * @param url URL 字符串
     * @return 清理后的安全 URL
     */
    public static String sanitizeUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }

        String cleanUrl = url.trim();

        // 移除 javascript: 协议
        if (cleanUrl.toLowerCase().startsWith("javascript:")) {
            return "";
        }

        // 移除 vbscript: 协议
        if (cleanUrl.toLowerCase().startsWith("vbscript:")) {
            return "";
        }

        // 移除 data: 协议（可能包含恶意代码）
        if (cleanUrl.toLowerCase().startsWith("data:")) {
            return "";
        }

        return cleanUrl;
    }

    /**
     * 验证文件名是否安全
     * @param filename 文件名
     * @return true 如果文件名安全
     */
    public static boolean isSafeFilename(String filename) {
        if (StringUtils.isBlank(filename)) {
            return false;
        }

        // 检查危险字符
        if (filename.contains("..") ||
            filename.contains("/") ||
            filename.contains("\\") ||
            filename.contains(":") ||
            filename.contains("*") ||
            filename.contains("?") ||
            filename.contains("\"") ||
            filename.contains("<") ||
            filename.contains(">") ||
            filename.contains("|")) {
            return false;
        }

        // 检查是否包含脚本
        return !containsXss(filename);
    }
}