package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gửi thư chào mừng <b>sau khi</b> giao dịch đăng ký tài khoản đã ghi thành công.
 *
 * <p>Cùng lý do với {@link BookingEmailListener}: {@code AFTER_COMMIT} bảo đảm
 * chỉ gửi thư khi tài khoản thật sự đã tồn tại trong CSDL, và lỗi gửi thư
 * (SMTP sai cấu hình, mất mạng...) không được phép làm hỏng việc đăng ký - lúc
 * listener này chạy thì {@code UserService.register} đã trả về xong xuôi rồi.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEmailListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserService.UserRegisteredEvent event) {
        try {
            emailService.sendWelcomeEmail(event.fullName(), event.email());
        } catch (RuntimeException e) {
            // Tài khoản đã ghi xong rồi. Thư không gửi được là chuyện đáng ghi log
            // để xử lý sau, không phải lý do để báo lỗi cho người vừa đăng ký.
            log.error("Không gửi được thư chào mừng tới {}: {}", event.email(), e.getMessage(), e);
        }
    }
}
