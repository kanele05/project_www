package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.edu.iuh.fit.tourbooking.entity.Booking;

@Component
@RequiredArgsConstructor
@Slf4j
// Nghe sự kiện đặt tour thành công (sau khi transaction commit) rồi gửi thư xác nhận.
public class BookingEmailListener {

    private final BookingService bookingService;
    private final EmailService emailService;

    // Gửi thư xác nhận đơn; lỗi gửi thư bị bắt và ghi log, không làm hỏng đơn đã đặt.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingPlaced(BookingService.BookingPlacedEvent event) {
        try {

            Booking booking = bookingService.getDetailByCode(event.bookingCode());
            emailService.sendBookingConfirmation(booking);

        } catch (RuntimeException e) {

            log.error("Không gửi được thư xác nhận cho đơn {}: {}",
                    event.bookingCode(), e.getMessage(), e);
        }
    }
}
