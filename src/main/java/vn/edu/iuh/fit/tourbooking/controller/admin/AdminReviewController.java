package vn.edu.iuh.fit.tourbooking.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.service.ReviewService;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

/**
 * Kiểm duyệt đánh giá tour (UC020).
 *
 * <p>Không dùng cột/enum mới: {@code Review.approved} đã được thiết kế sẵn cho
 * đúng việc này từ Phase 1 (xem Javadoc của trường đó), chỉ là trước UC020 chưa
 * có màn nào lật cờ này lên. Cùng khuôn PRG với các controller quản trị khác:
 * mọi POST kết thúc bằng {@code redirect:} + flash attribute.</p>
 */
@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;
    private final MessageHelper messages;

    /** Mặc định mở ở "Chờ duyệt" - đúng yêu cầu UC020, đây là hàng đợi việc phải làm. */
    @GetMapping
    public String list(@RequestParam(defaultValue = "PENDING") ReviewService.StatusFilter status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("reviewsPage", reviewService.adminList(status, page));
        model.addAttribute("status", status);
        model.addAttribute("statuses", ReviewService.StatusFilter.values());
        model.addAttribute("pageUrlPrefix", "/admin/reviews?status=" + status + "&");
        return "admin/review/list";
    }

    /** Duyệt: đánh giá bắt đầu hiện ra trang công khai và tính vào điểm trung bình. */
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        reviewService.approve(id);
        ra.addFlashAttribute("successMessage", messages.get("admin.review.approved"));
        return "redirect:/admin/reviews";
    }

    /** Bỏ duyệt: gỡ khỏi trang công khai mà không xoá, duyệt lại được bất cứ lúc nào. */
    @PostMapping("/{id}/unapprove")
    public String unapprove(@PathVariable Long id, RedirectAttributes ra) {
        reviewService.unapprove(id);
        ra.addFlashAttribute("successMessage", messages.get("admin.review.unapproved"));
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String replyText, RedirectAttributes ra) {
        try {
            reviewService.reply(id, replyText);
            ra.addFlashAttribute("successMessage", messages.get("admin.review.replied"));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", messages.of(e));
        }
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        reviewService.delete(id);
        ra.addFlashAttribute("successMessage", messages.get("admin.review.deleted"));
        return "redirect:/admin/reviews";
    }
}
