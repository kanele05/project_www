package vn.edu.iuh.fit.tourbooking.dto.view;

/**
 * Danh mục tour cho {@code GET /api/categories}.
 *
 * @param url địa chỉ trang danh mục, dựng sẵn ở máy chủ theo slug
 */
public record CategoryDto(Long id,
                          String name,
                          String slug,
                          String description,
                          String imageUrl,
                          String url,
                          boolean active) {
}
