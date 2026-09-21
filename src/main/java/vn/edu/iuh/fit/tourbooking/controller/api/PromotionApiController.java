package vn.edu.iuh.fit.tourbooking.controller.api;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;
import vn.edu.iuh.fit.tourbooking.service.CartService;
import vn.edu.iuh.fit.tourbooking.service.PromotionService;
import vn.edu.iuh.fit.tourbooking.session.Cart;

import java.math.BigDecimal;

/**
 * Web service kiểm mã giảm giá cho nút "Áp dụng" ở trang thanh toán.
 *
 * <p>Mã không dùng được thì {@code PromotionService} ném {@code BusinessRuleException},
 * và {@code ApiExceptionHandler} đổi nó thành <b>409 Conflict</b> kèm câu tiếng
 * Việt - giống hệt cách bốn quy tắc chặn xoá trả lời. Trình duyệt chỉ việc hiện
 * {@code message} lên, không phải tự dịch mã lỗi.</p>
 *
 * <p><b>Giá trị đơn hàng lấy từ giỏ trong Session, không nhận từ tham số.</b> Nếu
 * để trình duyệt gửi lên số tiền thì ai cũng khai được đơn 100 triệu để lấy mức
 * giảm tối đa. Đây cũng là lý do địa chỉ này không cần tham số nào ngoài mã.</p>
 */
@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionApiController {

    private final PromotionService promotionService;
    private final CartService cartService;

    /**
     * @param discount    số tiền được giảm
     * @param totalBefore tiền hàng trước khi giảm
     * @param totalAfter  số tiền khách thực trả
     */
    public record CouponResult(String code, String name,
                               BigDecimal discount,
                               BigDecimal totalBefore,
                               BigDecimal totalAfter) {
    }

    @GetMapping("/check")
    public CouponResult check(@RequestParam String code,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              HttpSession session) {
        Cart cart = cartService.getCart(session);
        BigDecimal before = cart.getTotalAmount();
        Long userId = principal == null ? null : principal.getId();

        PromotionService.CouponCheck result = promotionService.check(code, userId, before);
        BigDecimal discount = result.discount();

        return new CouponResult(result.getCode(), result.getName(),
                discount, before, before.subtract(discount));
    }
}
