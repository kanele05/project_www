package vn.edu.iuh.fit.tourbooking.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.repository.BookingPassengerRepository;
import vn.edu.iuh.fit.tourbooking.repository.BookingStatusHistoryRepository;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;
import vn.edu.iuh.fit.tourbooking.service.BookingService;
import vn.edu.iuh.fit.tourbooking.service.PaymentService;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

/**
 * Quản lý đơn đặt tour.
 *
 * <p>Không có chức năng xoá đơn: đơn hàng là chứng từ, cần huỷ thì đổi trạng
 * thái sang {@code CANCELLED} - vừa giữ được lịch sử, vừa trả lại chỗ cho đợt
 * khởi hành một cách có kiểm soát.</p>
 */
@Controller
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
@Validated
public class AdminBookingController {

    private final BookingService bookingService;
    private final BookingPassengerRepository bookingPassengerRepository;
    private final BookingStatusHistoryRepository bookingStatusHistoryRepository;
    private final PaymentService paymentService;
    private final MessageHelper messages;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) BookingStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("bookingsPage", bookingService.adminSearch(keyword, status, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("pageUrlPrefix", buildPageUrl(keyword, status));
        return "admin/booking/list";
    }

    @GetMapping("/{code}")
    public String detail(@PathVariable String code, Model model) {
        Booking booking = bookingService.getDetailByCode(code);
        model.addAttribute("booking", booking);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("passengers", bookingPassengerRepository.findByBookingId(booking.getId()));
        // Bổ sung A/B: dòng thời gian đổi trạng thái và các lần thanh toán.
        model.addAttribute("statusHistory", bookingStatusHistoryRepository.findByBookingCode(code));
        model.addAttribute("payments", paymentService.findByBookingId(booking.getId()));
        return "admin/booking/detail";
    }

    @PostMapping("/{code}/status")
    public String updateStatus(@PathVariable String code,
                               @RequestParam BookingStatus status,
                               @RequestParam(required = false) String reason,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes ra) {
        try {
            bookingService.updateStatus(code, status, principal.getId(), reason);
            // status.getDisplayName() là chuỗi tiếng Việt viết cứng trong enum - dùng
            // làm tham số cho bản dịch tiếng Anh thì câu tiếng Anh lòi ra tên trạng
            // thái tiếng Việt. messages.get(status.getMessageKey()) mới đúng: dịch
            // "PENDING" theo ngôn ngữ hiện tại trước khi lồng vào câu thông báo.
            ra.addFlashAttribute("successMessage",
                    messages.get("admin.booking.statusUpdated", messages.get(status.getMessageKey())));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", messages.of(e));
        }
        return "redirect:/admin/bookings/" + code;
    }

    /** Bổ sung B: đánh dấu đã thu tiền cho một lần thanh toán của đơn. */
    @PostMapping("/{code}/payments/{paymentId}/mark-paid")
    public String markPaid(@PathVariable String code,
                           @PathVariable Long paymentId,
                           @RequestParam(required = false) String txnRef,
                           RedirectAttributes ra) {
        try {
            paymentService.markPaid(paymentId, code, txnRef);
            ra.addFlashAttribute("successMessage", messages.get("admin.payment.marked"));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", messages.of(e));
        }
        return "redirect:/admin/bookings/" + code;
    }

    /**
     * Sửa số khách của một dòng trong đơn.
     *
     * <p>Sau thao tác này, thành tiền của dòng, tổng tiền của đơn và số chỗ còn
     * trống của đợt khởi hành đều được tính lại - xem
     * {@code BookingService.updateDetailQuantity}.</p>
     */
    @PostMapping("/{code}/details/{detailId}")
    public String updateDetail(@PathVariable String code,
                               @PathVariable Long detailId,
                               @RequestParam @jakarta.validation.constraints.Min(0) int numAdults,
                               @RequestParam @jakarta.validation.constraints.Min(0) int numChildren,
                               RedirectAttributes ra) {
        try {
            bookingService.updateDetailQuantity(code, detailId, numAdults, numChildren);
            ra.addFlashAttribute("successMessage", messages.get("admin.booking.detailUpdated"));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", messages.of(e));
        }
        return "redirect:/admin/bookings/" + code;
    }

    private String buildPageUrl(String keyword, BookingStatus status) {
        StringBuilder sb = new StringBuilder("/admin/bookings?");
        if (keyword != null && !keyword.isBlank()) {
            sb.append("keyword=").append(java.net.URLEncoder.encode(keyword,
                    java.nio.charset.StandardCharsets.UTF_8)).append('&');
        }
        if (status != null) {
            sb.append("status=").append(status.name()).append('&');
        }
        return sb.toString();
    }
}
