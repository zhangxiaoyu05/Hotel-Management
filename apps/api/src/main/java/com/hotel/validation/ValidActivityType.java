package com.hotel.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * 活动类型验证注解
 */
@Documented
@Constraint(validatedBy = ActivityTypeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidActivityType {
    String message() default "无效的活动类型";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}