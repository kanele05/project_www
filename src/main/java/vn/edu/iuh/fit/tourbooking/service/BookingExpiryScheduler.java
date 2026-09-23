package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.config.AppProperties;
import vn.edu.iuh.fit.tourbooking.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
// Bộ hẹn giờ chạy định kỳ, tự huỷ các đơn CHỜ đã quá hạn thanh toán (mục 12.4).
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final AppProperties appProperties;

    // Tìm các đơn PENDING quá hạn rồi lần lượt tự huỷ, ghi log số đơn xử lý/lỗi.
    @Scheduled(fixedDelay = 15, initialDelay = 1, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void expireUnpaidBookings() {
        LocalDateTime before = LocalDateTime.now().minusHours(appProperties.booking().pendingExpiryHours());
        List<Long> ids = bookingRepository.findExpiredPendingIds(before);
        if (ids.isEmpty()) {
            return;
        }

        log.info("Bộ hẹn giờ mục 12.4: {} đơn CHỜ quá {} giờ, bắt đầu tự huỷ",
                ids.size(), appProperties.booking().pendingExpiryHours());

        int failed = 0;
        for (Long id : ids) {
            try {
                bookingService.expirePendingBooking(id);
            } catch (Exception e) {
                failed++;
                log.error("Không tự huỷ được đơn quá hạn id={}: {}", id, e.getMessage(), e);
            }
        }
        log.info("Bộ hẹn giờ mục 12.4: xong - {} đơn xử lý, {} lỗi", ids.size(), failed);
    }
}
