package com.bn.berrynovel.service.Validator;

import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import com.bn.berrynovel.domain.dto.RegisterDTO;
import com.bn.berrynovel.service.UserService;

@Service
public class RegisterValidator implements ConstraintValidator<RegisterChecked, RegisterDTO> {
    // RegisterDTO is the data type being validated.
    private final UserService userService;

    public RegisterValidator(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean isValid(RegisterDTO user, ConstraintValidatorContext context) {
        boolean valid = true;

        // Check if password fields match
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            context.buildConstraintViolationWithTemplate("ComfirmPassword doesn't fit")
                    .addPropertyNode("confirmPassword")
                    // addPropertyNode("confirmPassword"): attach the error to confirmPassword
                    // in the RegisterDTO object, not password or email.
                    .addConstraintViolation() // ?
                    .disableDefaultConstraintViolation();
            // Disable the default error message and show only the custom message.
            valid = false;
        }

        // Additional validations can be added here
        // Check whether the email already exists.
        if (this.userService.checkEmailExists(user.getEmail())) {
            context.buildConstraintViolationWithTemplate("Email already exists")
                    .addPropertyNode("email")
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
            valid = false;
        }

        // Check whether the username already exists.
        if (this.userService.checkUsernameExists(user.getUsername())) {
            context.buildConstraintViolationWithTemplate("Username already exists")
                    .addPropertyNode("username")
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
