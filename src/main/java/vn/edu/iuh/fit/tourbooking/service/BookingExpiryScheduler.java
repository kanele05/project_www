package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.config.AppProperties;
import vn.edu.iuh.fit.tourbooking.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tự huỷ đơn CHỜ XÁC NHẬN chưa thanh toán quá hạn (SPEC_CHUNG.md mục 12.4).
 *
 * <p>Chạy định kỳ mỗi 15 phút. <b>Mỗi đơn được xử lý trong một giao dịch riêng</b>
 * ({@code BookingService.expirePendingBooking}, gọi qua bean khác nên proxy
 * {@code @Transactional} có hiệu lực) - lấy danh sách id ở một truy vấn
 * read-only rồi lặp qua từng id, bắt riêng lỗi của từng đơn để một đơn hỏng
 * (ví dụ xung đột khoá lạc quan với một thao tác khác đang chạm cùng đợt khởi
 * hành) không làm rớt cả mẻ đang xử lý.</p>
 *
 * <p>⚠️ Bẫy dữ liệu mẫu: nếu {@code database/03_seed_data.sql} có đơn CHỜ với
 * ngày đặt cố định trong quá khứ (thay vì tính tương đối theo lúc chạy script),
 * lần khởi động đầu tiên trên máy người chấm bộ hẹn giờ này sẽ huỷ sạch chúng -
 * xem phần đầu {@code 03_seed_data.sql} về cách tính ngày tương đối.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final AppProperties appProperties;

    /**
     * 15 phút một lần, tính từ sau khi lần chạy TRƯỚC kết thúc ({@code fixedDelay}
     * chứ không phải {@code fixedRate}): mẻ trước còn đang xử lý nhiều đơn thì mẻ
     * sau không chồng lên, tránh cùng lúc hai luồng chạm vào cùng một đơn.
     *
     * <p><b>Cố ý KHÔNG {@code @Transactional} ở phương thức này.</b> Không có giao
     * dịch bao ngoài thì {@code bookingRepository.findExpiredPendingIds(...)} tự
     * mở một giao dịch chỉ-đọc ngắn của riêng nó (hành vi mặc định của
     * {@code SimpleJpaRepository}), và mỗi lời gọi
     * {@code bookingService.expirePendingBooking(id)} (đi qua bean khác nên proxy
     * {@code @Transactional REQUIRED} có hiệu lực) cũng tự mở giao dịch MỚI của
     * riêng nó. Nếu thêm {@code @Transactional} ở đây, mọi lời gọi bên trong sẽ
     * gia nhập lại đúng MỘT giao dịch bao ngoài - đúng thứ mục 12.4 cấm.</p>
     */
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
