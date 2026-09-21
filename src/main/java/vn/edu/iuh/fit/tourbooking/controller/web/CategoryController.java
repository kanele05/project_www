package vn.edu.iuh.fit.tourbooking.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.iuh.fit.tourbooking.dto.form.TourSearchForm;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;
import vn.edu.iuh.fit.tourbooking.service.CategoryService;
import vn.edu.iuh.fit.tourbooking.service.TourService;

/**
 * Trang tour theo danh mục: {@code /categories/du-lich-bien-dao}.
 *
 * <p>Dùng lại đúng khuôn mẫu {@code tour/list} của trang danh sách, chỉ khác là
 * danh mục đã được cố định sẵn và có thêm phần tiêu đề giới thiệu danh mục.
 * Không chuyển hướng sang {@code /tours?categoryId=...} vì như vậy sẽ mất địa chỉ
 * dạng slug vốn dễ đọc và tốt cho tìm kiếm.</p>
 */
@Controller
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final TourService tourService;

    @GetMapping("/categories/{slug}")
    public String byCategory(@PathVariable String slug,
                             @ModelAttribute("searchForm") TourSearchForm form,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        TourCategory category = categoryService.getBySlug(slug);

        // Ép bộ lọc về đúng danh mục này, kể cả khi người dùng tự thêm
        // ?categoryId=... khác vào địa chỉ.
        form.setCategoryId(category.getId());

        model.addAttribute("category", category);
        model.addAttribute("toursPage", tourService.search(form, page));
        model.addAttribute("destinations", tourService.findAllDestinations());
        model.addAttribute("pageUrlPrefix", form.toQueryPrefix("/categories/" + slug));
        return "tour/list";
    }
}
