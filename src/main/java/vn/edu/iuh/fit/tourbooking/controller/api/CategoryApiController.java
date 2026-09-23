package vn.edu.iuh.fit.tourbooking.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.tourbooking.dto.view.CategoryDto;
import vn.edu.iuh.fit.tourbooking.mapper.CategoryMapper;
import vn.edu.iuh.fit.tourbooking.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
// REST: danh sách danh mục đang bật, dùng cho các ô chọn động.
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
