package vn.edu.iuh.fit.tourbooking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = UniqueEmailValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
// Ràng buộc mức lớp: email chưa có ai dùng (excludeIdField cho phép loại trừ chính bản ghi đang sửa).
public @interface UniqueEmail {

    String message() default "{validation.email.duplicate}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String emailField() default "email";

    String excludeIdField() default "";
}
