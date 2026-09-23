package vn.edu.iuh.fit.tourbooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ánh xạ nhóm cấu hình {@code app.*} trong application.yml thành đối tượng Java
 * có kiểu rõ ràng, thay vì rải rác {@code @Value("${...}")} khắp nơi.
 *
 * <p>Dùng record lồng nhau để cấu hình là bất biến (immutable) sau khi nạp.</p>
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Upload upload, Mail mail, Booking booking) {

    /**
     * @param dir       thư mục vật lý chứa ảnh upload (nằm ngoài classpath)
     * @param urlPrefix tiền tố URL công khai để trình duyệt tải ảnh, ví dụ {@code /uploads}
     */
    public record Upload(String dir, String urlPrefix) {
    }

    /**
     * @param mode     {@code console} = chỉ ghi log ra màn hình (mặc định khi phát triển),
     *                 {@code smtp} = gửi email thật qua máy chủ SMTP
     * @param from     địa chỉ email người gửi
     * @param fromName tên hiển thị của người gửi
     */
    public record Mail(String mode, String from, String fromName) {
    }

    /**
     * Các con số nghiệp vụ của vòng đời đơn hàng (SPEC_CHUNG.md mục 12) - đặt ở
     * đây thay vì viết cứng trong mã, để đổi được mà không cần biên dịch lại.
     *
     * @param cutoffDays           số ngày tối thiểu phải cách hôm nay mới đặt được một đợt
     *                             khởi hành (mục 12.5)
     * @param pendingExpiryHours   đơn CHỜ XÁC NHẬN chưa thanh toán quá số giờ này thì bị hệ
     *                             thống tự huỷ (mục 12.4)
     * @param selfCancelMinDays    khách chỉ tự huỷ được khi còn ít nhất ngần này ngày tới lúc
     *                             khởi hành (mục 12.3)
     * @param fullRefundMinDays    còn từ ngần này ngày trở lên thì khách tự huỷ được hoàn 100%
     *                             (mục 12.3)
     * @param partialRefundPercent tỉ lệ hoàn (%) khi khách tự huỷ trong khoảng
     *                             {@code [selfCancelMinDays, fullRefundMinDays)} (mục 12.3)
     */
    public record Booking(int cutoffDays, int pendingExpiryHours, int selfCancelMinDays,
                          int fullRefundMinDays, int partialRefundPercent) {
    }
}
