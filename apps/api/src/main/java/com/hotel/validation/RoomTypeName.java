package com.hotel.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * 房间类型名称验证注解
 */
@Documented
@Constraint(validatedBy = RoomTypeNameValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RoomTypeName {
    String message() default "房间类型名称格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}