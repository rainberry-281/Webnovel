package com.bn.berrynovel.service.Validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = RegisterValidator.class)
// gọi đến class "RegisterValidator.class"
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
// Quy định thời điểm annotation có hiệu lực.
// RUNTIME nghĩa là annotation sẽ được giữ lại khi chạy chương trình
@Documented
// Chỉ định rằng annotation này sẽ xuất hiện trong tài liệu Javadoc.
public @interface RegisterChecked {
    // @interface là từ khóa để tạo một annotation mới.
    // public giúp annotation có thể được sử dụng ở bất kỳ đâu trong project.
    // RegisterChecked là tên của annotation.
    String message() default "User register validation failed";

    // Cung cấp thông báo mặc định khi validation thất bại.
    Class<?>[] groups() default {}; // ?

    Class<? extends Payload>[] payload() default {}; // ?
}
