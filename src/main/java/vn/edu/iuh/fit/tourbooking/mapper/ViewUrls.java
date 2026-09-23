package vn.edu.iuh.fit.tourbooking.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.config.AppProperties;

@Component
@RequiredArgsConstructor
// Ghép đường dẫn hiển thị: ảnh (tuyệt đối cho ảnh seed, tương đối cho ảnh tải lên), tour, danh mục.
public class ViewUrls {

    private static final String NO_IMAGE = "/images/no-image.svg";

    private final AppProperties appProperties;

    // Ảnh không có thì trả ảnh mặc định; đường dẫn tuyệt đối (bắt đầu "/") giữ nguyên, còn lại ghép tiền tố thư mục upload.
    public String image(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return NO_IMAGE;
        }
        if (relativePath.startsWith("/")) {
            return relativePath;
        }
        return appProperties.upload().urlPrefix() + "/" + relativePath;
    }

    public String tourDetail(Long id, String slug) {
        return slug == null || slug.isBlank() ? "/tours/" + id : "/tours/" + id + "/" + slug;
    }

    public String category(String slug) {
        return "/categories/" + slug;
    }
}
