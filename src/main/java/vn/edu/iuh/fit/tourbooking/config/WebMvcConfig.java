package vn.edu.iuh.fit.tourbooking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cấu hình Spring MVC.
 *
 * <p>Việc duy nhất cần làm ở đây là công bố thư mục ảnh đã tải lên ra ngoài web.
 * Ảnh được lưu ở một thư mục nằm <b>ngoài</b> classpath (xem {@code AppProperties}),
 * nên Spring không tự phục vụ như các file trong {@code static/}.</p>
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(appProperties.upload().dir()).toAbsolutePath().normalize();

        // Dùng toUri().toString() chứ KHÔNG ghép chuỗi "file:" + đường dẫn.
        // Trên Windows đường dẫn có dấu gạch chéo ngược (C:\Users\...) và Spring sẽ
        // âm thầm không tìm ra file - ảnh mất mà không có lấy một dòng lỗi.
        // Kết quả của toUri() luôn kết thúc bằng dấu / nên các đường dẫn con nối đúng.
        String location = uploadDir.toUri().toString();

        registry.addResourceHandler(appProperties.upload().urlPrefix() + "/**")
                .addResourceLocations(location);

        log.info("Phục vụ ảnh tải lên: {}/** -> {}", appProperties.upload().urlPrefix(), location);
    }
}
