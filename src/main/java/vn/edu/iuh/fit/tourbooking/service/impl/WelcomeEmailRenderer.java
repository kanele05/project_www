package vn.edu.iuh.fit.tourbooking.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

/**
 * Dựng nội dung thư chào mừng sau khi đăng ký tài khoản.
 *
 * <p>Cùng cách làm với {@link BookingEmailRenderer}: nội dung dựng bằng chính
 * Thymeleaf đang dùng cho website ({@code templates/email/welcome.html}), để cả
 * bản in console lẫn bản gửi SMTP dùng chung một nơi sinh HTML.</p>
 */
@Component
@RequiredArgsConstructor
public class WelcomeEmailRenderer {

    private final SpringTemplateEngine templateEngine;
    private final MessageHelper messages;

    public String subject() {
        return messages.get("email.welcome.subject");
    }

    public String htmlBody(String fullName, String email) {
        Context context = new Context(LocaleContextHolder.getLocale());
        context.setVariable("fullName", fullName);
        context.setVariable("email", email);
        return templateEngine.process("email/welcome", context);
    }

    /**
     * Bản tóm tắt dạng chữ thuần cho nhật ký khi chạy ở chế độ console.
     *
     * <p>Cố ý <b>không</b> có mật khẩu trong bản tóm tắt này, kể cả khi tương
     * lai ai đó thêm trường mới vào form đăng ký.</p>
     */
    public String plainSummary(String fullName, String email) {
        return "Họ tên : " + fullName + "\nEmail  : " + email;
    }
}
