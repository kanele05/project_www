package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.edu.iuh.fit.tourbooking.entity.Booking;

/**
 * Gửi thư xác nhận <b>sau khi</b> giao dịch đặt tour đã ghi thành công.
 *
 * <p>{@code @TransactionalEventListener} với pha {@code AFTER_COMMIT} bảo đảm
 * điều đó: nếu giao dịch bị huỷ giữa chừng, tín hiệu coi như chưa từng phát ra
 * và không có thư nào được gửi. Đây là lý do phải dùng nó thay vì gọi thẳng
 * {@code emailService} trong {@code BookingService.placeOrder} - thư đã gửi đi
 * thì không thu hồi lại được, khác hẳn với một dòng dữ liệu có thể roll back.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEmailListener {

    private final BookingService bookingService;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingPlaced(BookingService.BookingPlacedEvent event) {
        try {
            // Nạp lại trong một giao dịch mới: đối tượng lấy từ giao dịch cũ đã bị
            // tách khỏi ngữ cảnh lưu trữ, đọc danh sách chi tiết sẽ ném
            // LazyInitializationException.
            Booking booking = bookingService.getDetailByCode(event.bookingCode());
            emailService.sendBookingConfirmation(booking);

        } catch (RuntimeException e) {
            // Đơn hàng đã ghi xong rồi. Thư không gửi được là chuyện đáng ghi log
            // để xử lý sau, không phải lý do để báo lỗi cho khách.
            log.error("Không gửi được thư xác nhận cho đơn {}: {}",
                    event.bookingCode(), e.getMessage(), e);
        }
    }
}
