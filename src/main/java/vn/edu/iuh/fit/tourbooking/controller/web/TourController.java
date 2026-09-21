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

/**
 * Danh sách tour và trang chi tiết tour.
 */
@Controller
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;
    private final ReviewService reviewService;

    /**
     * Trang danh sách: tìm kiếm, lọc, sắp xếp và phân trang.
     *
     * <p>Mọi tham số lọc được gom vào {@link TourSearchForm} qua
     * {@code @ModelAttribute}; đối tượng này cũng được đẩy sang view để các ô lọc
     * giữ nguyên giá trị người dùng vừa chọn sau khi tải lại trang.</p>
     */
    @GetMapping("/tours")
    public String list(@ModelAttribute("searchForm") TourSearchForm form,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("toursPage", tourService.search(form, page));
        model.addAttribute("destinations", tourService.findAllDestinations());
        // Địa chỉ dùng cho các nút phân trang, đã kèm sẵn mọi bộ lọc hiện tại -
        // nhờ vậy bấm sang trang 2 không làm mất từ khoá đang tìm.
        model.addAttribute("pageUrlPrefix", form.toQueryPrefix("/tours"));
        return "tour/list";
    }

    /**
     * Trang chi tiết một tour.
     *
     * <p>Địa chỉ dùng mã số kèm slug ({@code /tours/8/da-lat-...}) nhưng chỉ mã số
     * mới có ý nghĩa; slug chỉ để đường dẫn dễ đọc. Vì vậy có hai biến thể ánh xạ
     * cùng về một phương thức.</p>
     */
    @GetMapping({"/tours/{id}", "/tours/{id}/{slug}"})
    public String detail(@PathVariable Long id,
                         @RequestParam(defaultValue = "0") int page,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         Model model) {
        Tour tour = tourService.getDetail(id);
        // Trang danh sách đã lọc đúng tour đang bán, nhưng đường dẫn trực tiếp
        // /tours/{id} thì không đi qua bộ lọc đó - "Ngừng bán" phải chặn ở đây,
        // không thì khách vẫn mở được trang, thêm giỏ và đặt trọn luồng. Quản trị
        // viên vẫn xem được để còn "Bán lại" tour.
        boolean isAdmin = principal != null && principal.isAdmin();
        if (!tour.isActive() && !isAdmin) {
            throw ResourceNotFoundException.of("tour", id);
        }
        model.addAttribute("tour", tour);
        model.addAttribute("departures", tourService.findBookableDepartures(id));

        // ===== UC018: đánh giá tour =====
        // Đánh giá được nạp riêng (không join fetch chung với ảnh ở
        // TourService.getDetail) để tránh tích Descartes - cùng lý do departures
        // cũng được nạp riêng ở trên.
        model.addAttribute("reviewsPage", reviewService.findApprovedByTour(id, page));
        model.addAttribute("avgRating", reviewService.averageRating(id));
        model.addAttribute("reviewCount", reviewService.countApproved(id));
        model.addAttribute("pageUrlPrefix", "/tours/" + id + "?");

        Long userId = principal == null ? null : principal.getId();
        model.addAttribute("canReview", reviewService.canReview(userId, id));
        model.addAttribute("hasReviewed", reviewService.hasReviewed(userId, id));
        // UC020: hiện đánh giá của chính khách kèm nhãn trạng thái, kể cả khi
        // chưa duyệt (nên chưa xuất hiện trong reviewsPage ở trên).
        model.addAttribute("ownReview", reviewService.findOwnReview(userId, id).orElse(null));
        if (!model.containsAttribute("reviewForm")) {
            ReviewForm reviewForm = new ReviewForm();
            reviewForm.setTourId(id);
            model.addAttribute("reviewForm", reviewForm);
        }

        return "tour/detail";
    }
}
