package vn.edu.iuh.fit.tourbooking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kiểm tra hai ô mật khẩu khớp nhau.
 *
 * <p>Phải đặt ở mức lớp chứ không thể ở mức trường: ràng buộc này so sánh
 * <i>hai</i> trường với nhau, mà một validator ở mức trường chỉ nhìn thấy giá
 * trị của đúng trường nó gắn vào.</p>
 */
@Documented
@Constraint(validatedBy = PasswordsMatchValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordsMatch {

    String message() default "{validation.password.mismatch}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String passwordField() default "password";

    String confirmField() default "confirmPassword";
}
