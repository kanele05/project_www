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

/**
 * Khu vực tài khoản của khách hàng: hồ sơ, đổi mật khẩu, lịch sử đặt tour.
 *
 * <p>Cả nhánh {@code /account/**} đã ở mức {@code authenticated()} trong
 * {@code SecurityConfig}, nên trong này chắc chắn luôn có người dùng đăng nhập
 * và {@code principal} không bao giờ null.</p>
 *
 * <p><b>Nguyên tắc xuyên suốt:</b> mọi thao tác đều lấy mã người dùng từ
 * {@code principal.getId()}, tuyệt đối không nhận mã người dùng từ tham số gửi
 * lên. Nhận từ trình duyệt là mở cửa cho việc sửa hồ sơ hay xem đơn của người
 * khác chỉ bằng cách đổi một con số trên thanh địa chỉ.</p>
 */
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
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

    /**
     * Điền sẵn {@code profileForm} - kể cả {@code id} - <b>trước khi</b> Spring
     * gắn tham số request vào {@code @PostMapping("/profile")}.
     *
     * <p>{@code @Valid} chạy ngay lúc gắn tham số, trước cả khi thân phương thức
     * {@link #updateProfile} bắt đầu chạy; gọi {@code form.setId(...)} trong thân
     * phương thức là quá muộn, validator {@code @UniqueEmail(excludeIdField="id")}
     * đã kết luận xong với {@code id == null} (coi như thêm mới) từ trước đó.
     * Đưa việc điền {@code id} lên phương thức {@code @ModelAttribute} này thì
     * {@code id} có mặt <b>lúc</b> validator chạy, đúng như gotcha đã ghi trong
     * PLAN.md.</p>
     */
    @ModelAttribute("profileForm")
    public ProfileForm profileForm(@AuthenticationPrincipal CustomUserDetails principal) {
        return userService.toProfileForm(principal.getId());
    }

    /**
     * Chặn request tự gắn giá trị vào trường {@code id} của {@code profileForm}.
     *
     * <p>Không có dòng này thì {@code WebDataBinder} bind {@code id} từ tham số
     * request <b>trước khi</b> {@code @Valid} chạy, ghi đè lên giá trị vừa được
     * {@link #profileForm} điền sẵn từ tài khoản đang đăng nhập. Một request tự
     * chế {@code id=3&email=binh.tran@gmail.com} gửi bằng tài khoản khác (ví dụ
     * {@code an.nguyen}) khiến {@code @UniqueEmail(excludeIdField="id")} loại trừ
     * nhầm id 3 (đúng người đang sở hữu email đó) khỏi phép kiểm trùng - validator
     * báo "còn dùng được", request đi tiếp tới {@link #updateProfile}, nơi
     * {@code form.setId(principal.getId())} sửa lại id đúng nhưng đã quá muộn:
     * ghi email trùng xuống CSDL đụng ràng buộc UNIQUE, ra thẳng trang 500.
     * {@code setDisallowedFields} chặn ngay từ bước bind, nên {@code id} luôn giữ
     * đúng giá trị đã điền sẵn suốt vòng đời request.</p>
     */
    @InitBinder("profileForm")
    public void initProfileFormBinder(WebDataBinder binder) {
        binder.setDisallowedFields("id");
    }

    // ===================== Hồ sơ =====================

    @GetMapping("/profile")
    public String profilePage(Model model) {
        return "account/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult binding,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                RedirectAttributes ra) {
        // Ghi đè mã số bằng mã của tài khoản đang đăng nhập. Trường id trong biểu
        // mẫu chỉ phục vụ ràng buộc @UniqueEmail(excludeIdField), không phải để
        // người dùng chỉ định mình muốn sửa hồ sơ của ai.
        form.setId(principal.getId());

        if (binding.hasErrors()) {
            return "account/profile";
        }

        try {
            userService.updateProfile(principal.getId(), form);
            ra.addFlashAttribute("successMessage", getMessage("account.profile.updated"));
            return "redirect:/account/profile";

        } catch (BusinessRuleException e) {
            // Chốt chặn thứ hai của tính duy nhất email (xem UserService.updateProfile):
            // gắn lỗi vào đúng ô email thay vì để lọt xuống một trang 500.
            binding.rejectValue("email", "error", resolve(e));
            return "account/profile";
        }
    }

    // ===================== Đổi mật khẩu =====================

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
            // Sai mật khẩu hiện tại: gắn lỗi vào đúng ô đó thay vì hiện một dải
            // thông báo đỏ ở đầu trang.
            binding.rejectValue("currentPassword", "error", resolve(e));
            return "account/password";
        }
    }

    // ===================== Đơn đặt tour =====================

    @GetMapping("/bookings")
    public String bookings(@AuthenticationPrincipal CustomUserDetails principal,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        model.addAttribute("bookingsPage", bookingService.findByUser(principal.getId(), page));
        model.addAttribute("pageUrlPrefix", "/account/bookings?");
        return "account/bookings";
    }

    /**
     * Chi tiết một đơn.
     *
     * <p>Địa chỉ dùng <b>mã đơn</b> chứ không dùng khoá chính tuần tự, và
     * {@code getOwnedByCode} còn kiểm tra chủ sở hữu: khách hàng khác mở đúng mã
     * đơn này sẽ nhận <b>403</b>.</p>
     */
    @GetMapping("/bookings/{code}")
    public String bookingDetail(@PathVariable String code,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                Model model) {
        var booking = bookingService.getOwnedByCode(code, principal.getId(), principal.isAdmin());
        model.addAttribute("booking", booking);
        // Danh sách hành khách của cả đơn (có thể gồm nhiều dòng nếu đặt nhiều tour).
        model.addAttribute("passengers", bookingPassengerRepository.findByBookingId(booking.getId()));
        // Bổ sung A/B: dòng thời gian đổi trạng thái và các lần thanh toán.
        model.addAttribute("statusHistory", bookingStatusHistoryRepository.findByBookingCode(code));
        model.addAttribute("payments", paymentService.findByBookingId(booking.getId()));
        return "account/booking-detail";
    }

    // ===================== Đánh giá tour (UC018) =====================

    /**
     * Gửi đánh giá cho một tour. Đặt ở {@code /account/**} (đã ở mức
     * {@code authenticated()} trong {@code SecurityConfig}) chứ không đặt dưới
     * {@code /tours/**}: nhánh đó đang {@code permitAll()} không phân biệt
     * phương thức, đặt ở đây là cách rẻ nhất để bắt buộc đăng nhập mà không phải
     * sửa cấu hình bảo mật.
     */
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

    // ---------------------------------------------------------------------

    private String resolve(BusinessRuleException e) {
        return messageSource.getMessage(e.getMessageKey(), e.getArgs(),
                LocaleContextHolder.getLocale());
    }

    private String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}
