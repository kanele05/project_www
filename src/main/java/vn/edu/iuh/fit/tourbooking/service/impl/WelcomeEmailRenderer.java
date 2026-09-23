package vn.edu.iuh.fit.tourbooking.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

@Component
@RequiredArgsConstructor
// Dựng tiêu đề, nội dung HTML (qua Thymeleaf) và bản tóm tắt văn bản của thư chào mừng.
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

    public String plainSummary(String fullName, String email) {
        return "Họ tên : " + fullName + "\nEmail  : " + email;
    }
}
