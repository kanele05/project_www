package vn.edu.iuh.fit.tourbooking.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;

/**
 * Đổi mã thông điệp thành câu chữ theo ngôn ngữ đang dùng.
 *
 * <p>Gần như controller nào cũng cần đúng hai việc này, nên gom lại đây thay vì
 * mỗi lớp tự tiêm {@code MessageSource} rồi viết lại cùng hai phương thức riêng.
 * {@code LocaleContextHolder} lấy ngôn ngữ của <i>yêu cầu hiện tại</i>, nhờ vậy
 * khi bổ sung tiếng Anh ở giai đoạn sau thì mọi thông báo tự chuyển ngữ theo.</p>
 */
@Component
@RequiredArgsConstructor
public class MessageHelper {

    private final MessageSource messageSource;

    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    /** Đổi lỗi nghiệp vụ thành câu thông báo cho người dùng. */
    public String of(BusinessRuleException e) {
        return get(e.getMessageKey(), e.getArgs());
    }
}
