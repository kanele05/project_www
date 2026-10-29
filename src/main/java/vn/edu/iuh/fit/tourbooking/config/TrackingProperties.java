package vn.edu.iuh.fit.tourbooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Cấu hình ghi hành vi người dùng (tiền tố "app.tracking."): bật/tắt, hạn mức, trần thời gian xem, số ngày lưu.
@ConfigurationProperties(prefix = "app.tracking")
public record TrackingProperties(@DefaultValue("true") boolean enabled,
                                 @DefaultValue("60") int maxEventsPerMinute,
                                 @DefaultValue("500") int minDurationMs,
                                 @DefaultValue("1800000") int maxDurationMs,
                                 @DefaultValue("180") int retentionDays) {
}
