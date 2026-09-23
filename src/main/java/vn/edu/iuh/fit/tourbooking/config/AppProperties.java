package vn.edu.iuh.fit.tourbooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Gom các cấu hình riêng của ứng dụng (application.yml, tiền tố "app.") thành một record kiểu mạnh.
@ConfigurationProperties(prefix = "app")
public record AppProperties(Upload upload, Mail mail, Booking booking) {

    public record Upload(String dir, String urlPrefix) {
    }

    public record Mail(String mode, String from, String fromName) {
    }

    public record Booking(int cutoffDays, int pendingExpiryHours, int selfCancelMinDays,
                          int fullRefundMinDays, int partialRefundPercent) {
    }
}
