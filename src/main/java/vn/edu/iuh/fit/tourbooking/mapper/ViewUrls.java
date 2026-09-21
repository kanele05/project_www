package vn.edu.iuh.fit.tourbooking.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.config.AppProperties;

/**
 * Dựng các đường dẫn mà web service gửi ra cho trình duyệt.
 *
 * <p>Gom về một chỗ vì đây là những quy tắc rất dễ bị chép sai: thiếu một dấu
 * gạch chéo thì ảnh không hiện, mà lỗi lại im lặng. Tiền tố ảnh đọc từ
 * {@code app.upload.url-prefix} thay vì viết cứng, nên đổi cấu hình là mọi phản
 * hồi JSON đổi theo.</p>
 *
 * <p>Là bean chứ không phải lớp tiện ích toàn phương thức tĩnh, chính vì nó cần
 * đọc cấu hình. Các khuôn mẫu Thymeleaf vẫn tự ghép {@code /uploads/...} theo
 * cách riêng của chúng - chỗ đó chạy trước khi lớp này ra đời và không bắt buộc
 * phải đổi.</p>
 */
@Component
@RequiredArgsConstructor
public class ViewUrls {

    /** Ảnh dùng khi bản ghi chưa có ảnh nào - nằm trong {@code static/images}. */
    private static final String NO_IMAGE = "/images/no-image.svg";

    private final AppProperties appProperties;

    /**
     * Đổi đường dẫn tương đối lưu trong CSDL (ví dụ {@code tours/abc.png}) thành
     * địa chỉ tải được.
     *
     * <p>Giá trị bắt đầu bằng {@code /} là ảnh tĩnh của dữ liệu mẫu, không phải
     * file người dùng tải lên - giữ nguyên, đừng ghép thêm tiền tố.</p>
     */
    public String image(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return NO_IMAGE;
        }
        if (relativePath.startsWith("/")) {
            return relativePath;
        }
        return appProperties.upload().urlPrefix() + "/" + relativePath;
    }

    /** Trang chi tiết tour: {@code /tours/{id}/{slug}}. */
    public String tourDetail(Long id, String slug) {
        return slug == null || slug.isBlank() ? "/tours/" + id : "/tours/" + id + "/" + slug;
    }

    /** Trang danh mục: {@code /categories/{slug}}. */
    public String category(String slug) {
        return "/categories/" + slug;
    }
}
