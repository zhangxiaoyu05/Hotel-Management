package com.hotel.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * 价格范围验证注解
 */
@Documented
@Constraint(validatedBy = ValidPriceRangeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPriceRange {
    String message() default "价格必须在有效范围内（0.01-999999.99）";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}