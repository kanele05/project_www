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
        // Mục 12.1: màn quản trị chỉ liệt kê các trạng thái ĐÍCH hợp lệ của đơn
        // đang xem, không phải toàn bộ enum - chọn một lựa chọn ngoài danh sách
        // này chắc chắn bị BookingService.updateStatus từ chối.
        model.addAttribute("validNextStatuses", bookingService.validNextStatuses(booking));
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
     *
     * <p>Mục 12.7: cùng một địa chỉ này phục vụ CẢ HAI bước của luồng giữ bất
     * biến hành khách - lần gửi đầu (từ ô số lượng) không kèm
     * {@code newAdultNames}/{@code newChildNames}; nếu tăng số khách,
     * {@code BookingService} không áp dụng gì cả và trả về phần tên còn thiếu,
     * controller đẩy vào flash để trang chi tiết hiện đúng bấy nhiêu ô nhập tên
     * ngay dưới dòng đang sửa; lần gửi thứ hai (từ chính các ô đó, cùng địa
     * chỉ) mới thực sự ghi.</p>
     */
    @PostMapping("/{code}/details/{detailId}")
    public String updateDetail(@PathVariable String code,
                               @PathVariable Long detailId,
                               @RequestParam @jakarta.validation.constraints.Min(0) int numAdults,
                               @RequestParam @jakarta.validation.constraints.Min(0) int numChildren,
                               @RequestParam(required = false) java.util.List<String> newAdultNames,
                               @RequestParam(required = false) java.util.List<String> newChildNames,
                               RedirectAttributes ra) {
        try {
            BookingService.PassengerNameGap gap = bookingService.updateDetailQuantity(
                    code, detailId, numAdults, numChildren, newAdultNames, newChildNames);
            if (gap.isEmpty()) {
                // Nhẹ - 7 (đã tái hiện): giảm số khách tự động bỏ bớt hành khách
                // mà không nêu tên ai - quản trị viên phải tự đoán ai vừa "biến
                // mất". Nêu đích danh ngay trong thông báo thành công.
                String successMessage = messages.get("admin.booking.detailUpdated");
                if (!gap.droppedNames().isEmpty()) {
                    successMessage = successMessage + " "
                            + messages.get("admin.booking.passengers.dropped",
                                    String.join(", ", gap.droppedNames()));
                }
                ra.addFlashAttribute("successMessage", successMessage);
            } else {
                // Chưa ghi gì cả - còn thiếu tên. Giữ lại đúng detailId/số khách
                // đang chờ để trang chi tiết render tiếp phần "nhập tên" (xem
                // admin/booking/detail.html, khối pendingDetailId).
                ra.addFlashAttribute("errorMessage", messages.get("admin.booking.passengers.needNames"));
                ra.addFlashAttribute("pendingDetailId", detailId);
                ra.addFlashAttribute("pendingAdults", numAdults);
                ra.addFlashAttribute("pendingChildren", numChildren);
                ra.addFlashAttribute("pendingAdultsNeeded", gap.adultsNeeded());
                ra.addFlashAttribute("pendingChildrenNeeded", gap.childrenNeeded());
            }
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
