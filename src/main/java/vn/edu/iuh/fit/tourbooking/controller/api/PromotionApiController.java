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

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
// REST AJAX: kiểm mã khuyến mãi ngay tại trang giỏ hàng/checkout trước khi đặt tour.
public class PromotionApiController {

    private final PromotionService promotionService;
    private final CartService cartService;

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
