package vn.edu.iuh.fit.tourbooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ánh xạ nhóm cấu hình {@code app.*} trong application.yml thành đối tượng Java
 * có kiểu rõ ràng, thay vì rải rác {@code @Value("${...}")} khắp nơi.
 *
 * <p>Dùng record lồng nhau để cấu hình là bất biến (immutable) sau khi nạp.</p>
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Upload upload, Mail mail) {

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
}
