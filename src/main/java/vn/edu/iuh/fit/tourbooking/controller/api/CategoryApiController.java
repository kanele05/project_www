package vn.edu.iuh.fit.tourbooking.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.tourbooking.dto.view.CategoryDto;
import vn.edu.iuh.fit.tourbooking.mapper.CategoryMapper;
import vn.edu.iuh.fit.tourbooking.service.CategoryService;

import java.util.List;

/**
 * Web service về danh mục tour.
 *
 * <p>Chỉ công bố danh mục <b>đang hiển thị</b>. Danh mục bị ẩn vẫn còn trong CSDL
 * nhưng là chuyện nội bộ của khu vực quản trị - địa chỉ này ai cũng gọi được nên
 * không có lý do gì để lộ ra.</p>
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryApiController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @GetMapping
    public List<CategoryDto> list() {
        return categoryService.findActiveCategories().stream()
                .map(categoryMapper::toDto)
                .toList();
    }
}
