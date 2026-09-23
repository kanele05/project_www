package vn.edu.iuh.fit.tourbooking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.Set;

/**
 * Phần thực thi của {@link ValidPaymentMethod}.
 *
 * <p>Danh sách giá trị hợp lệ được dựng từ đúng ba khoá mà {@code checkout.html}
 * dùng để đổ vào thẻ {@code <option>} ({@code checkout.payment.bank/cash/momo}),
 * tra cả hai ngôn ngữ đang có (vi, en) - không phụ thuộc ngôn ngữ của request
 * hiện tại, vì trang thanh toán có thể được mở bằng tiếng này rồi gửi form sau
 * khi người dùng đã đổi ngôn ngữ ở tab khác.</p>
 */
@RequiredArgsConstructor
public class ValidPaymentMethodValidator implements ConstraintValidator<ValidPaymentMethod, String> {

    private static final String[] KEYS = {
            "checkout.payment.bank", "checkout.payment.cash", "checkout.payment.momo"
    };

    private final MessageSource messageSource;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Ô trống thì để @NotBlank lo, tránh báo hai lỗi chồng nhau trên cùng một ô.
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
