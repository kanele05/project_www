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

/**
 * Bản cài đặt gửi thư thật qua SMTP, chọn bằng {@code app.mail.mode = smtp}.
 *
 * <p>Cần khai báo thêm {@code spring.mail.username} và {@code spring.mail.password}
 * (với Gmail phải dùng "mật khẩu ứng dụng", không dùng mật khẩu đăng nhập).</p>
 */
@Service
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "smtp")
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final BookingEmailRenderer renderer;
    private final WelcomeEmailRenderer welcomeRenderer;

    @Override
    public void sendBookingConfirmation(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true thứ nhất: thư nhiều phần; tham số thứ hai: bảng mã, thiếu là
            // tiếng Việt trong thư thành dấu hỏi.
            MimeMessageHelper helper = new MimeMessageHelper(message, true,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(appProperties.mail().from(), appProperties.mail().fromName());
            helper.setTo(booking.getCustomerEmail());
            helper.setSubject(renderer.subject(booking));
            helper.setText(renderer.htmlBody(booking), true);   // true = nội dung HTML

            mailSender.send(message);
            log.info("Đã gửi thư xác nhận đơn {} tới {}", booking.getCode(), booking.getCustomerEmail());

        } catch (jakarta.mail.MessagingException | UnsupportedEncodingException e) {
            // Cố tình KHÔNG ném lại lỗi: đơn hàng đã được ghi vào CSDL rồi, không
            // thể vì máy chủ thư trục trặc mà báo khách là đặt tour thất bại.
            // Ghi log để xử lý sau là đủ.
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
            // Cũng cố tình KHÔNG ném lại lỗi: tài khoản đã được tạo xong, lỗi gửi
            // thư không được phép làm hỏng việc đăng ký.
            log.error("Không gửi được thư chào mừng tới {}: {}", email, e.getMessage());
        }
    }
}
