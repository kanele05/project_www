package vn.edu.iuh.fit.tourbooking.controller.web;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;
import vn.edu.iuh.fit.tourbooking.service.CategoryService;
import vn.edu.iuh.fit.tourbooking.service.RecommendationService;
import vn.edu.iuh.fit.tourbooking.service.TourService;

import java.util.List;

@Controller
@RequiredArgsConstructor
// Trang chủ công khai: tour nổi bật, danh mục, số liệu giới thiệu trang web.
public class HomeController {

    private static final int FEATURED_LIMIT = 6;

    private static final int DESTINATION_LIMIT = 14;

    private final TourService tourService;
    private final CategoryService categoryService;
    private final RecommendationService recommendationService;

    private static final int RECOMMEND_LIMIT = 3;

    @GetMapping("/")
    public String home(Model model, HttpSession session,
                       @AuthenticationPrincipal CustomUserDetails principal) {
        model.addAttribute("featuredTours", tourService.findFeatured(FEATURED_LIMIT));

        model.addAttribute("categoryCards", categoryService.findActiveWithTourCount());

        model.addAttribute("stats", tourService.siteStats());

        RecommendationService.Recommendation picks = recommendationService.recommend(
                principal == null ? null : principal.getId(), session.getId(), RECOMMEND_LIMIT);
        model.addAttribute("recommendedTours", picks.tours());
        model.addAttribute("recommendSource", picks.source().name());

        List<String> destinations = tourService.findAllDestinations();
        model.addAttribute("destinations",
                destinations.subList(0, Math.min(destinations.size(), DESTINATION_LIMIT)));

        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("stats", tourService.siteStats());
        model.addAttribute("categoryCards", categoryService.findActiveWithTourCount());
        return "about";
    }
}
