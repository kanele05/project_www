package vn.edu.iuh.fit.tourbooking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import vn.edu.iuh.fit.tourbooking.repository.UserRepository;

@RequiredArgsConstructor
// Kiểm email trùng qua UserRepository, tự loại trừ id hiện tại khi sửa hồ sơ.
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

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(emailField)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
