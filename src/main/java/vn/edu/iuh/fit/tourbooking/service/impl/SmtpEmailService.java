package vn.edu.iuh.fit.tourbooking.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.tourbooking.config.AppProperties;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.service.EmailService;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "smtp")
@RequiredArgsConstructor
@Slf4j
// Bản cài đặt EmailService gửi thư thật qua SMTP (JavaMailSender), bật bằng app.mail.mode=smtp.
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final BookingEmailRenderer renderer;
    private final WelcomeEmailRenderer welcomeRenderer;

    @Override
    public void sendBookingConfirmation(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(appProperties.mail().from(), appProperties.mail().fromName());
            helper.setTo(booking.getCustomerEmail());
            helper.setSubject(renderer.subject(booking));
            helper.setText(renderer.htmlBody(booking), true);

            mailSender.send(message);
            log.info("Đã gửi thư xác nhận đơn {} tới {}", booking.getCode(), booking.getCustomerEmail());

        } catch (jakarta.mail.MessagingException | UnsupportedEncodingException e) {

            log.error("Không gửi được thư xác nhận cho đơn {}: {}", booking.getCode(), e.getMessage());
        }
    }

    @Override
    public void sendWelcomeEmail(String fullName, String email) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(appProperties.mail().from(), appProperties.mail().fromName());
            helper.setTo(email);
            helper.setSubject(welcomeRenderer.subject());
            helper.setText(welcomeRenderer.htmlBody(fullName, email), true);

            mailSender.send(message);
            log.info("Đã gửi thư chào mừng tới {}", email);

        } catch (jakarta.mail.MessagingException | UnsupportedEncodingException e) {

            log.error("Không gửi được thư chào mừng tới {}: {}", email, e.getMessage());
        }
    }
}
