package com.bn.berrynovel.service.Validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Check that the password has uppercase, lowercase, and numeric characters.
        if (value == null) {
            return false;
        }
        return value.matches("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,}$");
    }
}
