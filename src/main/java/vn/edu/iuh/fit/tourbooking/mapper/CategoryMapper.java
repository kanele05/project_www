package vn.edu.iuh.fit.tourbooking.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.dto.view.CategoryDto;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;

@Component
@RequiredArgsConstructor
// Chuyển TourCategory sang DTO hiển thị, ghép URL ảnh và đường dẫn danh mục.
public class CategoryMapper {

    private final ViewUrls urls;

    public CategoryDto toDto(TourCategory category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                urls.image(category.getImageUrl()),
                urls.category(category.getSlug()),
                category.isActive());
    }
}
