package vn.edu.iuh.fit.tourbooking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import vn.edu.iuh.fit.tourbooking.repository.UserRepository;

/**
 * Phần thực thi của {@link UniqueEmail}.
 *
 * <p>Lớp validator ở Spring Boot cũng là một bean, nên tiêm thẳng repository vào
 * được. Ràng buộc đặt ở mức lớp (chứ không phải mức trường) vì nó cần đọc hai
 * trường cùng lúc: email và mã người dùng đang sửa.</p>
 */
@RequiredArgsConstructor
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, Object> {

    private final UserRepository userRepository;

    private String emailField;
    private String excludeIdField;
    private String message;

    @Override
    public void initialize(UniqueEmail annotation) {
        this.emailField = annotation.emailField();
        this.excludeIdField = annotation.excludeIdField();
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(Object form, ConstraintValidatorContext context) {
        BeanWrapper wrapper = new BeanWrapperImpl(form);
        Object email = wrapper.getPropertyValue(emailField);

        // Ô trống thì để @NotBlank lo, ở đây không báo thêm lỗi thứ hai cho cùng
        // một ô - hai dòng chữ đỏ chồng nhau chỉ làm người dùng rối.
        if (email == null || email.toString().isBlank()) {
            return true;
        }

        Long excludeId = null;
        if (!excludeIdField.isBlank()) {
            excludeId = (Long) wrapper.getPropertyValue(excludeIdField);
        }

        boolean duplicated = excludeId == null
                ? userRepository.existsByEmail(email.toString())
                : userRepository.existsByEmailAndIdNot(email.toString(), excludeId);

        if (duplicated) {
            // Gắn lỗi vào đúng ô email thay vì để ở mức cả biểu mẫu, nhờ vậy
            // Thymeleaf tô đỏ được đúng ô người dùng cần sửa.
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(emailField)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
