package com.bn.berrynovel.service.Validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // kiểm tra sring email có ký tự in hoa, ký tự đặc biệt và số
        return value.matches("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,}$");
    }
}
