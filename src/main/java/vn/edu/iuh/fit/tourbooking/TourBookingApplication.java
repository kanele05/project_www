package vn.edu.iuh.fit.tourbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Điểm khởi động của ứng dụng "Website giới thiệu tour du lịch và đăng ký tour trực tuyến".
 *
 * <p>Bài tập lớn môn Lập trình WWW (Java) - Khoa Công nghệ thông tin,
 * Trường Đại học Công nghiệp TP. Hồ Chí Minh.</p>
 *
 * <ul>
 *   <li>{@code @EnableJpaAuditing} bật cơ chế tự điền {@code createdAt}/{@code updatedAt}
 *       cho các entity kế thừa {@code Auditable}.</li>
 *   <li>{@code @ConfigurationPropertiesScan} cho phép nạp {@code AppProperties}
 *       (nhóm cấu hình bắt đầu bằng {@code app.*}) mà không cần khai báo thêm.</li>
 *   <li>{@code @EnableScheduling} bật {@code @Scheduled} - dùng cho
 *       {@code BookingExpiryScheduler} (mục 12.4: tự huỷ đơn CHỜ quá hạn thanh toán).</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
@EnableScheduling
public class TourBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TourBookingApplication.class, args);
    }
}
