package vn.edu.iuh.fit.tourbooking.controller.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.tourbooking.dto.form.CheckoutForm;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;
import vn.edu.iuh.fit.tourbooking.service.BookingService;
import vn.edu.iuh.fit.tourbooking.service.CartService;
import vn.edu.iuh.fit.tourbooking.service.UserService;
import vn.edu.iuh.fit.tourbooking.session.Cart;

/**
 * Thanh toán - biến giỏ hàng trong session thành một đơn hàng trong CSDL.
 *
 * <p>Địa chỉ {@code /checkout} đã được {@code SecurityConfig} đặt ở mức
 * {@code authenticated()}: khách chưa đăng nhập sẽ bị đưa sang trang đăng nhập,
 * và sau khi đăng nhập xong quay lại đúng đây (nhờ {@code defaultSuccessUrl} đặt
 * tham số thứ hai là false). Giỏ hàng vẫn còn nguyên vì phiên chỉ bị đổi mã chứ
 * không bị tạo lại.</p>
 */
@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final BookingService bookingService;
    private final UserService userService;
    private final MessageSource messageSource;

    @GetMapping("/checkout")
    public String checkoutPage(@AuthenticationPrincipal CustomUserDetails principal,
                               HttpSession session,
                               Model model) {
        Cart cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        // Điền sẵn thông tin từ hồ sơ cho đỡ phải gõ lại, nhưng vẫn cho sửa:
        // người đặt tour không nhất thiết là người đi.
        if (!model.containsAttribute("checkoutForm")) {
            model.addAttribute("checkoutForm", prefill(principal.getId()));
        }
        return "checkout/checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@Valid @ModelAttribute("checkoutForm") CheckoutForm form,
                             BindingResult binding,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             HttpSession session,
                             RedirectAttributes ra) {
        Cart cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        if (binding.hasErrors()) {
            return "checkout/checkout";
        }

        try {
            Booking booking = bookingService.placeOrder(principal.getId(), form, cart);

            // Xoá giỏ hàng CHỈ SAU KHI giao dịch đã commit xong. Đây chính là dòng
            // hiện thực yêu cầu "Session được xoá về null" của đề bài.
            cartService.clear(session);

            return "redirect:/checkout/success/" + booking.getCode();

        } catch (BusinessRuleException e) {
            // Ví dụ: trong lúc khách còn đang điền biểu mẫu thì người khác đã đặt
            // hết chỗ. Giỏ hàng giữ nguyên để khách sửa lại số lượng.
            ra.addFlashAttribute("errorMessage", messageSource.getMessage(
                    e.getMessageKey(), e.getArgs(), LocaleContextHolder.getLocale()));
            return "redirect:/cart";
        }
    }

    /**
     * Trang báo đặt tour thành công.
     *
     * <p>Vẫn đi qua {@code getOwnedByCode} để kiểm tra quyền sở hữu: đây là một
     * địa chỉ có mã đơn nằm ngay trên thanh địa chỉ, không thể vì nó tên là
     * "success" mà bỏ qua bước kiểm tra.</p>
     */
    @GetMapping("/checkout/success/{code}")
    public String success(@PathVariable String code,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          Model model) {
        model.addAttribute("booking",
                bookingService.getOwnedByCode(code, principal.getId(), principal.isAdmin()));
        return "checkout/success";
    }

    // ---------------------------------------------------------------------

    private CheckoutForm prefill(Long userId) {
        User user = userService.getById(userId);
        CheckoutForm form = new CheckoutForm();
        form.setCustomerName(user.getFullName());
        form.setCustomerEmail(user.getEmail());
        form.setCustomerPhone(user.getPhone());
        form.setCustomerAddress(user.getAddress());
        form.setPaymentMethod("Chuyển khoản ngân hàng");
        return form;
    }
}
