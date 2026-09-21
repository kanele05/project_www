package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;

/**
 * Biểu mẫu thêm / sửa danh mục tour.
 *
 * <p>Không có ô nhập slug: slug được sinh tự động từ tên bằng {@code SlugUtil}.
 * Bắt người dùng tự gõ slug chỉ tạo cơ hội gõ sai (dấu cách, chữ hoa, tiếng Việt
 * có dấu) và làm hỏng đường dẫn.</p>
 */
@Data
public class CategoryForm {

    private Long id;

    @NotBlank(message = "{validation.category.name.required}")
    @Size(max = 100, message = "{validation.category.name.size}")
    private String name;

    @Size(max = 500, message = "{validation.category.description.size}")
    private String description;

    private boolean active = true;

    private MultipartFile imageFile;

    /** Đường dẫn ảnh hiện tại, chỉ để hiển thị bản xem trước. */
    private String currentImageUrl;

    public static CategoryForm from(TourCategory category) {
        CategoryForm form = new CategoryForm();
        form.setId(category.getId());
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        form.setActive(category.isActive());
        form.setCurrentImageUrl(category.getImageUrl());
        return form;
    }
}
