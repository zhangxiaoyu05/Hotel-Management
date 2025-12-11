package com.hotel.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

/**
 * 价格范围验证器
 */
public class ValidPriceRangeValidator implements ConstraintValidator<ValidPriceRange, BigDecimal> {

    // 最小价格：0.01元
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    // 最大价格：999999.99元
    private static final BigDecimal MAX_PRICE = new BigDecimal("999999.99");

    @Override
    public boolean isValid(BigDecimal price, ConstraintValidatorContext context) {
        if (price == null) {
            return false; // 价格不能为空
        }

        // 验证价格范围
        return price.compareTo(MIN_PRICE) >= 0 && price.compareTo(MAX_PRICE) <= 0;
    }
}