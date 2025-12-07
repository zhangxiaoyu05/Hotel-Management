package com.hotel.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 房间类型名称验证器
 */
public class RoomTypeNameValidator implements ConstraintValidator<RoomTypeName, String> {

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        if (name == null || name.trim().isEmpty()) {
            return false; // 房间类型名称不能为空
        }

        String trimmedName = name.trim();

        // 长度验证：2-50个字符
        if (trimmedName.length() < 2 || trimmedName.length() > 50) {
            return false;
        }

        // 格式验证：允许中文、英文、数字、空格、括号、横线
        // 不能包含特殊字符如：@#$%^&*等
        String pattern = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s\\(\\)\\-（）]+$";

        return trimmedName.matches(pattern);
    }
}