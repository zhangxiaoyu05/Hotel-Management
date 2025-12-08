package com.hotel.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Phone {
    String message() default "电话号码格式不正确，请输入有效的手机号码或固定电话";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}