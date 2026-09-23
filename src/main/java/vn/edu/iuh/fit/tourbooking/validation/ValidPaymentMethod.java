package vn.edu.iuh.fit.tourbooking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidPaymentMethodValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
// Ràng buộc mức trường: giá trị phải khớp đúng một trong ba câu dịch hình thức thanh toán.
public @interface ValidPaymentMethod {

    String message() default "{validation.paymentMethod.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
