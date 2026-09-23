package vn.edu.iuh.fit.tourbooking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.tourbooking.config.AppProperties;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.service.EmailService;

/**
 * Bản cài đặt dùng khi phát triển và khi trình bày: chỉ in thư ra màn hình.
 *
 * <p>Được chọn khi {@code app.mail.mode = console} (và cả khi không khai báo gì,
 * nhờ {@code matchIfMissing = true} - quên cấu hình thì rơi vào phương án an
 * toàn, chứ không phải phương án gửi thư thật).</p>
 *
 * <p>Vẫn dựng nội dung HTML đầy đủ chứ không bỏ qua: nếu mẫu thư có lỗi cú pháp
 * Thymeleaf, lỗi sẽ lộ ra ngay từ bây giờ chứ không đợi tới lúc bật SMTP.</p>
 */
@Service
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "console", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ConsoleEmailService implements EmailService {

    private final AppProperties appProperties;
    private final BookingEmailRenderer renderer;
    private final WelcomeEmailRenderer welcomeRenderer;

    @Override
    public void sendBookingConfirmation(Booking booking) {
        String html = renderer.htmlBody(booking);

        log.info("""

                        ==================== [EMAIL] ====================
                        Từ      : {} <{}>
                        Đến     : {}
                        Tiêu đề : {}
                        -------------------------------------------------
                        {}
                        -------------------------------------------------
                        (Đã dựng xong nội dung HTML, {} ký tự. Đặt app.mail.mode=smtp để gửi thật.)
                        =================================================
                        """,
                appProperties.mail().fromName(), appProperties.mail().from(),
                booking.getCustomerEmail(),
                renderer.subject(booking),
                renderer.plainSummary(booking),
                html.length());
    }

    @Override
    public void sendWelcomeEmail(String fullName, String email) {
        String html = welcomeRenderer.htmlBody(fullName, email);

        log.info("""

                        ==================== [EMAIL] ====================
                        Từ      : {} <{}>
                        Đến     : {}
                        Tiêu đề : {}
                        -------------------------------------------------
                        {}
                        -------------------------------------------------
                        (Đã dựng xong nội dung HTML, {} ký tự. Đặt app.mail.mode=smtp để gửi thật.)
                        =================================================
                        """,
                appProperties.mail().fromName(), appProperties.mail().from(),
                email,
                welcomeRenderer.subject(),
                welcomeRenderer.plainSummary(fullName, email),
                html.length());
    }
}
