package vn.edu.iuh.fit.tourbooking.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.iuh.fit.tourbooking.service.ContactService;
import vn.edu.iuh.fit.tourbooking.service.ReviewService;

@ControllerAdvice(basePackages = "vn.edu.iuh.fit.tourbooking.controller.admin")
@RequiredArgsConstructor
// Bơm số liệu dùng chung (huy hiệu đánh giá chờ duyệt, liên hệ mới) vào mọi trang khu quản trị.
public class AdminModelAdvice {

    private final ReviewService reviewService;
    private final ContactService contactService;

    @ModelAttribute("pendingReviewCount")
    public long pendingReviewCount() {
        return reviewService.countPending();
    }

    @ModelAttribute("newContactCount")
    public long newContactCount() {
        return contactService.countNew();
    }
}
