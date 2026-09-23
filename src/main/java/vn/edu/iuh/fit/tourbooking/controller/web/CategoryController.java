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

@Controller
@RequiredArgsConstructor
// Trang công khai theo danh mục tour (lọc tour theo slug danh mục).
public class CategoryController {

    private final CategoryService categoryService;
    private final TourService tourService;

    @GetMapping("/categories/{slug}")
    public String byCategory(@PathVariable String slug,
                             @ModelAttribute("searchForm") TourSearchForm form,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        TourCategory category = categoryService.getBySlug(slug);

        form.setCategoryId(category.getId());

        model.addAttribute("category", category);
        model.addAttribute("toursPage", tourService.search(form, page));
        model.addAttribute("destinations", tourService.findAllDestinations());
        model.addAttribute("pageUrlPrefix", form.toQueryPrefix("/categories/" + slug));
        return "tour/list";
    }
}
