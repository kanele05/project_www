package vn.edu.iuh.fit.tourbooking.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.tourbooking.dto.form.ChangePasswordForm;
import vn.edu.iuh.fit.tourbooking.dto.form.ProfileForm;
import vn.edu.iuh.fit.tourbooking.dto.form.ReviewForm;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.repository.BookingPassengerRepository;
import vn.edu.iuh.fit.tourbooking.repository.BookingStatusHistoryRepository;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;
import vn.edu.iuh.fit.tourbooking.service.BookingService;
import vn.edu.iuh.fit.tourbooking.service.PaymentService;
import vn.edu.iuh.fit.tourbooking.service.ReviewService;
import vn.edu.iuh.fit.tourbooking.service.UserService;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
// Khu vực tài khoản của khách hàng: hồ sơ, đổi mật khẩu, lịch sử đặt tour, gửi đánh giá, tự huỷ đơn.
public class AccountController {

    private final UserService userService;
    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final BookingPassengerRepository bookingPassengerRepository;
    private final BookingStatusHistoryRepository bookingStatusHistoryRepository;
    private final PaymentService paymentService;
    private final MessageSource messageSource;

    @GetMapping
    public String index() {
        return "redirect:/account/bookings";
    }

    @ModelAttribute("profileForm")
    public ProfileForm profileForm(@AuthenticationPrincipal CustomUserDetails principal) {
        return userService.toProfileForm(principal.getId());
    }

    @InitBinder("profileForm")
    public void initProfileFormBinder(WebDataBinder binder) {
        binder.setDisallowedFields("id");
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        return "account/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult binding,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                RedirectAttributes ra) {

        form.setId(principal.getId());

        if (binding.hasErrors()) {
            return "account/profile";
        }

        try {
            userService.updateProfile(principal.getId(), form);
            ra.addFlashAttribute("successMessage", getMessage("account.profile.updated"));
            return "redirect:/account/profile";

        } catch (BusinessRuleException e) {

            binding.rejectValue("email", "error", resolve(e));
            return "account/profile";
        }
    }

    @GetMapping("/password")
    public String passwordPage(Model model) {
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new ChangePasswordForm());
        }
        return "account/password";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") ChangePasswordForm form,
                                 BindingResult binding,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes ra) {
        if (binding.hasErrors()) {
            return "account/password";
        }
        try {
            userService.changePassword(principal.getId(), form);
            ra.addFlashAttribute("successMessage", getMessage("account.password.updated"));
            return "redirect:/account/password";

        } catch (BusinessRuleException e) {

            binding.rejectValue("currentPassword", "error", resolve(e));
            return "account/password";
        }
    }

    @GetMapping("/bookings")
    public String bookings(@AuthenticationPrincipal CustomUserDetails principal,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        model.addAttribute("bookingsPage", bookingService.findByUser(principal.getId(), page));
        model.addAttribute("pageUrlPrefix", "/account/bookings?");
        return "account/bookings";
    }

    @GetMapping("/bookings/{code}")
    public String bookingDetail(@PathVariable String code,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                Model model) {
        var booking = bookingService.getOwnedByCode(code, principal.getId(), principal.isAdmin());
        model.addAttribute("booking", booking);

        model.addAttribute("passengers", bookingPassengerRepository.findByBookingId(booking.getId()));

        model.addAttribute("statusHistory", bookingStatusHistoryRepository.findByBookingCode(code));
        model.addAttribute("payments", paymentService.findByBookingId(booking.getId()));

        if (booking.isCancellable()) {
            model.addAttribute("selfCancelPolicy", bookingService.evaluateSelfCancel(booking));
        }
        return "account/booking-detail";
    }

    @PostMapping("/bookings/{code}/cancel")
    public String selfCancel(@PathVariable String code,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes ra) {
        try {
            bookingService.cancelBySelf(code, principal.getId());
            ra.addFlashAttribute("successMessage", getMessage("account.bookings.selfCancel.success"));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", resolve(e));
        }
        return "redirect:/account/bookings/" + code;
    }

    @PostMapping("/reviews")
    public String submitReview(@Valid @ModelAttribute("reviewForm") ReviewForm form,
                               BindingResult binding,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes ra) {
        if (binding.hasErrors()) {
            ra.addFlashAttribute("errorMessage", getMessage("error.review.invalid"));
            return "redirect:/tours/" + form.getTourId();
        }
        try {
            reviewService.submit(principal.getId(), form);
            ra.addFlashAttribute("successMessage", getMessage("account.review.submitted"));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", resolve(e));
        }
        return "redirect:/tours/" + form.getTourId();
    }

    private String resolve(BusinessRuleException e) {
        return messageSource.getMessage(e.getMessageKey(), e.getArgs(),
                LocaleContextHolder.getLocale());
    }

    private String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}
