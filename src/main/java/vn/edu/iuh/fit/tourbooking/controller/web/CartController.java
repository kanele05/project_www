package vn.edu.iuh.fit.tourbooking.controller.web;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.service.CartService;
import vn.edu.iuh.fit.tourbooking.session.CartItem;

/**
 * Giỏ hàng.
 *
 * <p>Mọi phương thức POST đều kết thúc bằng {@code redirect:} kèm flash attribute
 * (mẫu POST - Redirect - GET). Nếu trả thẳng tên khuôn mẫu, người dùng bấm F5 sau
 * khi thêm vào giỏ sẽ thêm lần nữa mà không hề hay biết.</p>
 */
@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** Dùng để đổi mã thông điệp trong {@link BusinessRuleException} ra câu chữ. */
    private final MessageSource messageSource;

    @GetMapping("/cart")
    public String view() {
        // Giỏ hàng đã được GlobalModelAdvice đẩy sẵn vào model.
        return "cart/cart";
    }

    /**
     * Thêm vào giỏ. Hỗ trợ cả hai lối vào của đề bài:
     * <ul>
     *   <li>từ <b>trang chi tiết</b> - biểu mẫu gửi kèm {@code departureId} khách đã chọn;</li>
     *   <li>từ <b>trang danh sách</b> - chỉ có {@code tourId}, hệ thống tự chọn
     *       đợt khởi hành gần nhất còn chỗ.</li>
     * </ul>
     */
    @PostMapping("/cart/add")
    public String add(@RequestParam(required = false) Long departureId,
                      @RequestParam(required = false) Long tourId,
                      @RequestParam(defaultValue = "1") int numAdults,
                      @RequestParam(defaultValue = "0") int numChildren,
                      HttpSession session,
                      RedirectAttributes ra) {
        try {
            CartItem item = departureId != null
                    ? cartService.addByDeparture(session, departureId, numAdults, numChildren)
                    : cartService.addByTour(session, tourId, numAdults, numChildren);
            ra.addFlashAttribute("successMessage",
                    getMessage("cart.added", item.getTourName()));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", resolve(e));
        }
        return "redirect:/cart";
    }

    /**
     * Sửa số lượng khách của một dòng.
     *
     * <p>Tổng số khách bằng 0 thì {@code CartService} xoá hẳn dòng khỏi giỏ - đúng
     * yêu cầu "số lượng bằng 0 thì xoá" của đề bài.</p>
     */
    @PostMapping("/cart/update")
    public String update(@RequestParam Long departureId,
                         @RequestParam int numAdults,
                         @RequestParam int numChildren,
                         HttpSession session,
                         RedirectAttributes ra) {
        try {
            boolean removed = cartService.updateQuantity(session, departureId, numAdults, numChildren);
            ra.addFlashAttribute("successMessage",
                    getMessage(removed ? "cart.removedByZero" : "cart.updated"));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", resolve(e));
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam Long departureId,
                         HttpSession session,
                         RedirectAttributes ra) {
        cartService.remove(session, departureId);
        ra.addFlashAttribute("successMessage", getMessage("cart.removed"));
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clear(HttpSession session, RedirectAttributes ra) {
        cartService.clear(session);
        ra.addFlashAttribute("successMessage", getMessage("cart.cleared"));
        return "redirect:/cart";
    }

    // ---------------------------------------------------------------------

    /** Đổi mã thông điệp của lỗi nghiệp vụ thành câu chữ theo ngôn ngữ hiện tại. */
    private String resolve(BusinessRuleException e) {
        return messageSource.getMessage(e.getMessageKey(), e.getArgs(),
                LocaleContextHolder.getLocale());
    }

    private String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}
