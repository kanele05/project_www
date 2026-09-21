package vn.edu.iuh.fit.tourbooking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Objects;

/**
 * Phần thực thi của {@link PasswordsMatch}.
 *
 * <p>Viết theo kiểu tổng quát (nhận tên trường qua tham số) để dùng lại được cho
 * cả biểu mẫu đăng ký lẫn biểu mẫu đổi mật khẩu.</p>
 */
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

        // Báo lỗi ở ô nhập lại mật khẩu - đó mới là ô người dùng cần sửa.
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(confirmField)
                .addConstraintViolation();
        return false;
    }
}
