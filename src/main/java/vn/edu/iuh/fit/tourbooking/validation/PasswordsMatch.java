package vn.edu.iuh.fit.tourbooking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PasswordsMatchValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
// Ràng buộc mức lớp: hai trường mật khẩu (mặc định password/confirmPassword) phải khớp nhau.
public @interface PasswordsMatch {

    String message() default "{validation.password.mismatch}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String passwordField() default "password";

    String confirmField() default "confirmPassword";
}
