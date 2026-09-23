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

@Controller
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
@Validated
// Khu quản trị: danh sách/chi tiết đơn hàng, đổi trạng thái, sửa số khách, thao tác thanh toán.
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

        model.addAttribute("validNextStatuses", bookingService.validNextStatuses(booking));
        model.addAttribute("passengers", bookingPassengerRepository.findByBookingId(booking.getId()));

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

            ra.addFlashAttribute("successMessage",
                    messages.get("admin.booking.statusUpdated", messages.get(status.getMessageKey())));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", messages.of(e));
        }
        return "redirect:/admin/bookings/" + code;
    }

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

                String successMessage = messages.get("admin.booking.detailUpdated");
                if (!gap.droppedNames().isEmpty()) {
                    successMessage = successMessage + " "
                            + messages.get("admin.booking.passengers.dropped",
                                    String.join(", ", gap.droppedNames()));
                }
                ra.addFlashAttribute("successMessage", successMessage);
            } else {

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
