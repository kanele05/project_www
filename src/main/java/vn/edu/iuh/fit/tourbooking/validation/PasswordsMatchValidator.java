package vn.edu.iuh.fit.tourbooking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Objects;

// Kiểm hai trường mật khẩu bằng reflection (BeanWrapper), gắn lỗi vào đúng trường xác nhận.
public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, Object> {

    private String passwordField;
    private String confirmField;
    private String message;

    @Override
    public void initialize(PasswordsMatch annotation) {
        this.passwordField = annotation.passwordField();
        this.confirmField = annotation.confirmField();
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(Object form, ConstraintValidatorContext context) {
        BeanWrapper wrapper = new BeanWrapperImpl(form);
        Object password = wrapper.getPropertyValue(passwordField);
        Object confirm = wrapper.getPropertyValue(confirmField);

        if (Objects.equals(password, confirm)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(confirmField)
                .addConstraintViolation();
        return false;
    }
}
