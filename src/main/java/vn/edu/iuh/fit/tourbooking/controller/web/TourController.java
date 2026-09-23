package vn.edu.iuh.fit.tourbooking.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.iuh.fit.tourbooking.dto.form.ReviewForm;
import vn.edu.iuh.fit.tourbooking.dto.form.TourSearchForm;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;
import vn.edu.iuh.fit.tourbooking.service.ReviewService;
import vn.edu.iuh.fit.tourbooking.service.TourService;

@Controller
@RequiredArgsConstructor
// Trang công khai về tour: danh sách có lọc/phân trang, chi tiết tour kèm đánh giá.
public class TourController {

    private final TourService tourService;
    private final ReviewService reviewService;

    @GetMapping("/tours")
    public String list(@ModelAttribute("searchForm") TourSearchForm form,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("toursPage", tourService.search(form, page));
        model.addAttribute("destinations", tourService.findAllDestinations());

        model.addAttribute("pageUrlPrefix", form.toQueryPrefix("/tours"));
        return "tour/list";
    }

    @GetMapping({"/tours/{id}", "/tours/{id}/{slug}"})
    public String detail(@PathVariable Long id,
                         @RequestParam(defaultValue = "0") int page,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         Model model) {
        Tour tour = tourService.getDetail(id);

        boolean isAdmin = principal != null && principal.isAdmin();
        if (!tour.isActive() && !isAdmin) {
            throw ResourceNotFoundException.of("tour", id);
        }
        model.addAttribute("tour", tour);
        model.addAttribute("departures", tourService.findBookableDepartures(id));

        model.addAttribute("reviewsPage", reviewService.findApprovedByTour(id, page));
        model.addAttribute("avgRating", reviewService.averageRating(id));
        model.addAttribute("reviewCount", reviewService.countApproved(id));
        model.addAttribute("pageUrlPrefix", "/tours/" + id + "?");

        Long userId = principal == null ? null : principal.getId();
        model.addAttribute("canReview", reviewService.canReview(userId, id));
        model.addAttribute("hasReviewed", reviewService.hasReviewed(userId, id));

        model.addAttribute("ownReview", reviewService.findOwnReview(userId, id).orElse(null));
        if (!model.containsAttribute("reviewForm")) {
            ReviewForm reviewForm = new ReviewForm();
            reviewForm.setTourId(id);
            model.addAttribute("reviewForm", reviewForm);
        }

        return "tour/detail";
    }
}
