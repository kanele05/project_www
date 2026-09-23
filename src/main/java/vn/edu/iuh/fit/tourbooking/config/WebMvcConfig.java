package vn.edu.iuh.fit.tourbooking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
@Slf4j
// Đăng ký thư mục upload ngoài classpath thành đường dẫn tĩnh /uploads/** (dùng URI có "/" cuối, an toàn trên Windows).
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(appProperties.upload().dir()).toAbsolutePath().normalize();

        String location = uploadDir.toUri().toString();

        registry.addResourceHandler(appProperties.upload().urlPrefix() + "/**")
                .addResourceLocations(location);

        log.info("Phục vụ ảnh tải lên: {}/** -> {}", appProperties.upload().urlPrefix(), location);
    }
}
