package com.hotel.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

/**
 * 活动类型验证器
 */
public class ActivityTypeValidator implements ConstraintValidator<ValidActivityType, String> {

    private static final List<String> ALLOWED_ACTIVITY_TYPES = Arrays.asList(
        "DOUBLE_POINTS",
        "REVIEW_CONTEST",
        "MONTHLY_CHAMPION"
    );

    @Override
    public boolean isValid(String activityType, ConstraintValidatorContext context) {
        if (activityType == null || activityType.trim().isEmpty()) {
            return false;
        }

        return ALLOWED_ACTIVITY_TYPES.contains(activityType.trim().toUpperCase());
    }
}