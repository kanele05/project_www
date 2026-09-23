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

@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
// Khu quản trị: kiểm duyệt đánh giá tour (duyệt/bỏ duyệt/trả lời/xoá) - UC020.
public class AdminReviewController {

    private final ReviewService reviewService;
    private final MessageHelper messages;

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

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        reviewService.approve(id);
        ra.addFlashAttribute("successMessage", messages.get("admin.review.approved"));
        return "redirect:/admin/reviews";
    }

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
