package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
// Nghe sự kiện đăng ký tài khoản (sau khi transaction commit) rồi gửi thư chào mừng.
public class UserEmailListener {

    private final EmailService emailService;

    // Gửi thư chào mừng; lỗi gửi thư bị bắt và ghi log, không làm hỏng việc đăng ký.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserService.UserRegisteredEvent event) {
        try {
            emailService.sendWelcomeEmail(event.fullName(), event.email());
        } catch (RuntimeException e) {

            log.error("Không gửi được thư chào mừng tới {}: {}", event.email(), e.getMessage(), e);
        }
    }
}
