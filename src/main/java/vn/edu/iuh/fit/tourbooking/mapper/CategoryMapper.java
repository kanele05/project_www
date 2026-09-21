package vn.edu.iuh.fit.tourbooking.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.dto.view.CategoryDto;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;

/**
 * Đổi entity {@link TourCategory} sang DTO.
 *
 * <p>{@code TourCategory} cố ý không map collection tour nào nên phép chuyển đổi
 * này an toàn tuyệt đối với {@code open-in-view = false}.</p>
 */
@Component
@RequiredArgsConstructor
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
