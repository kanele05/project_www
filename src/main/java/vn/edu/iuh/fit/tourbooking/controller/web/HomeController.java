package vn.edu.iuh.fit.tourbooking.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.iuh.fit.tourbooking.service.CategoryService;
import vn.edu.iuh.fit.tourbooking.service.TourService;

import java.util.List;

/**
 * Trang chủ và trang giới thiệu.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    /** Sáu tour nổi bật xếp thành hai hàng ba cột. */
    private static final int FEATURED_LIMIT = 6;

    /** Số điểm đến hiện ở dải "khám phá theo điểm đến". */
    private static final int DESTINATION_LIMIT = 14;

    private final TourService tourService;
    private final CategoryService categoryService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredTours", tourService.findFeatured(FEATURED_LIMIT));

        // Danh mục kèm số tour: một câu truy vấn gộp nhóm, không lặp đếm từng
        // danh mục (xem TourRepository.countActiveGroupedByCategory).
        model.addAttribute("categoryCards", categoryService.findActiveWithTourCount());

        // Số liệu thật, đọc từ CSDL - không viết cứng con số đẹp vào khuôn mẫu.
        model.addAttribute("stats", tourService.siteStats());

        List<String> destinations = tourService.findAllDestinations();
        model.addAttribute("destinations",
                destinations.subList(0, Math.min(destinations.size(), DESTINATION_LIMIT)));

        return "index";
    }

    /**
     * Trang giới thiệu.
     *
     * <p>Dùng lại đúng bộ số liệu của trang chủ thay vì đếm lại theo cách khác:
     * hai trang nói hai con số khác nhau về cùng một thứ là lỗi rất mất uy tín, và
     * cực kỳ dễ xảy ra khi mỗi trang tự truy vấn riêng.</p>
     */
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("stats", tourService.siteStats());
        model.addAttribute("categoryCards", categoryService.findActiveWithTourCount());
        return "about";
    }
}
