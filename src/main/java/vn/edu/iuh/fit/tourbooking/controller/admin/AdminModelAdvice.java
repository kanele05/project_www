package vn.edu.iuh.fit.tourbooking.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.iuh.fit.tourbooking.service.ContactService;
import vn.edu.iuh.fit.tourbooking.service.ReviewService;

/**
 * Bơm sẵn những thứ mọi trang trong khu vực quản trị đều cần: huy hiệu đếm số
 * đánh giá đang chờ duyệt (UC020) và số liên hệ mới chưa xử lý (UC022) trên
 * menu bên trái.
 *
 * <p>Cùng vai trò với {@code controller.web.GlobalModelAdvice} nhưng tách riêng
 * vì hai khu vực có nhu cầu khác nhau - xem chú thích ở đó.</p>
 */
@ControllerAdvice(basePackages = "vn.edu.iuh.fit.tourbooking.controller.admin")
@RequiredArgsConstructor
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
