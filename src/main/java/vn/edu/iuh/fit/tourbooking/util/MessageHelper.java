package vn.edu.iuh.fit.tourbooking.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;

@Component
@RequiredArgsConstructor
// Lấy câu dịch theo ngôn ngữ hiện tại của request, dùng ở tầng service (nơi không có LocaleResolver trực tiếp).
public class MessageHelper {

    private final MessageSource messageSource;

    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    public String of(BusinessRuleException e) {
        return get(e.getMessageKey(), e.getArgs());
    }
}
