package vn.edu.iuh.fit.tourbooking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.Set;

@RequiredArgsConstructor
// So khớp giá trị với câu dịch checkout.payment.bank/cash/momo ở CẢ hai ngôn ngữ, không phụ thuộc locale hiện tại.
public class ValidPaymentMethodValidator implements ConstraintValidator<ValidPaymentMethod, String> {

    private static final String[] KEYS = {
            "checkout.payment.bank", "checkout.payment.cash", "checkout.payment.momo"
    };

    private final MessageSource messageSource;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null || value.isBlank()) {
            return true;
        }

        Set<String> allowed = Set.of(
                messageSource.getMessage(KEYS[0], null, Locale.of("vi")),
                messageSource.getMessage(KEYS[1], null, Locale.of("vi")),
                messageSource.getMessage(KEYS[2], null, Locale.of("vi")),
                messageSource.getMessage(KEYS[0], null, Locale.ENGLISH),
                messageSource.getMessage(KEYS[1], null, Locale.ENGLISH),
                messageSource.getMessage(KEYS[2], null, Locale.ENGLISH));

        return allowed.contains(value.trim());
    }
}
