package vn.edu.iuh.fit.tourbooking.dto.view;

// DTO hiển thị danh mục tour.
public record CategoryDto(Long id,
                          String name,
                          String slug,
                          String description,
                          String imageUrl,
                          String url,
                          boolean active) {
}
