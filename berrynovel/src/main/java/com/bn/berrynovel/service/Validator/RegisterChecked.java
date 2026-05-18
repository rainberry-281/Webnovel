package com.bn.berrynovel.service.Validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = RegisterValidator.class)
// Use the RegisterValidator class.
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
// Define when the annotation is available.
// RUNTIME means the annotation is retained while the program runs.
@Documented
// Include this annotation in Javadoc.
public @interface RegisterChecked {
    // @interface declares a new annotation.
    // public allows the annotation to be used anywhere in the project.
    // RegisterChecked is the annotation name.
    String message() default "User register validation failed";

    // Default message when validation fails.
    Class<?>[] groups() default {}; // ?

    Class<? extends Payload>[] payload() default {}; // ?
}
