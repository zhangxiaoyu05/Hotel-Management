package com.hotel.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 数据脱敏工具类
 * 用于对敏感数据进行脱敏处理
 *
 * @author Hotel System
 * @version 1.0
 */
@Slf4j
@Component
public class DataMaskingUtil {

    // 手机号正则表达式
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    // 邮箱正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    /**
     * 脱敏手机号
     * 保留前3位和后4位，中间用*代替
     * 例：13812345678 -> 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 脱敏邮箱
     * 保留第一个字符和@后面的域名，中间用*代替
     * 例：example@email.com -> e******@email.com
     */
    public static String maskEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }

        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        // 保留第一个字符，其余用*代替
        String maskedUsername = username.charAt(0) + "*".repeat(username.length() - 1);
        return maskedUsername + domain;
    }

    /**
     * 脱敏身份证号
     * 保留前6位和后4位，中间用*代替
     * 例：110101199001011234 -> 110101********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    /**
     * 脱敏姓名
     * 保留姓氏，名字用*代替
     * 例：张三 -> 张*
     */
    public static String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /**
     * 脱敏银行卡号
     * 保留前4位和后4位，中间用*代替
     * 例：6222020200012345678 -> 6222************5678
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + "*".repeat(bankCard.length() - 8) + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 脱敏地址
     * 保留前3个字符，其余用*代替
     */
    public static String maskAddress(String address) {
        if (address == null || address.length() <= 3) {
            return address;
        }
        return address.substring(0, 3) + "*".repeat(address.length() - 3);
    }

    /**
     * 通用脱敏方法
     * 根据数据类型自动选择脱敏策略
     */
    public static String mask(String data, String type) {
        if (data == null || data.trim().isEmpty()) {
            return data;
        }

        try {
            switch (type.toLowerCase()) {
                case "phone":
                case "mobile":
                    return maskPhone(data);
                case "email":
                    return maskEmail(data);
                case "idcard":
                case "id":
                    return maskIdCard(data);
                case "name":
                case "username":
                    return maskName(data);
                case "bankcard":
                case "card":
                    return maskBankCard(data);
                case "address":
                    return maskAddress(data);
                default:
                    log.warn("未知的数据脱敏类型: {}, 返回原始数据", type);
                    return data;
            }
        } catch (Exception e) {
            log.error("数据脱敏失败，类型: {}, 数据: {}", type, data, e);
            return data; // 脱敏失败时返回原数据
        }
    }

    /**
     * 批量脱敏
     */
    public static String[] maskBatch(String[] dataArray, String type) {
        if (dataArray == null) {
            return null;
        }

        String[] maskedArray = new String[dataArray.length];
        for (int i = 0; i < dataArray.length; i++) {
            maskedArray[i] = mask(dataArray[i], type);
        }
        return maskedArray;
    }
}