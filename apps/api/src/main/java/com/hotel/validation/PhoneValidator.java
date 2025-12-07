package com.hotel.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<Phone, String> {

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null || phone.trim().isEmpty()) {
            return true; // 允许为空，如果需要必填可以使用@NotBlank
        }

        String trimmedPhone = phone.trim();

        // 支持中国手机号码和固定电话格式
        // 手机号：1开头，第二位3-9，总共11位数字
        String mobilePattern = "^1[3-9]\\d{9}$";

        // 固定电话：区号（3-4位）-号码（7-8位），或者直接7-8位号码
        String landlinePattern = "^0\\d{2,3}-?\\d{7,8}$|^\\d{7,8}$";

        return trimmedPhone.matches(mobilePattern) || trimmedPhone.matches(landlinePattern);
    }
}