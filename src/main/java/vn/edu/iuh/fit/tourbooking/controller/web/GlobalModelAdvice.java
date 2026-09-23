package vn.edu.iuh.fit.tourbooking.controller.web;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;
import vn.edu.iuh.fit.tourbooking.service.CartService;
import vn.edu.iuh.fit.tourbooking.service.CategoryService;
import vn.edu.iuh.fit.tourbooking.session.Cart;

import java.util.List;

@ControllerAdvice(basePackages = "vn.edu.iuh.fit.tourbooking.controller.web")
@RequiredArgsConstructor
// Bơm dữ liệu dùng chung (giỏ hàng, câu chữ cho JavaScript) vào mọi trang công khai.
public class GlobalModelAdvice {

    private final CartService cartService;
    private final CategoryService categoryService;

    @ModelAttribute("cart")
    public Cart cart(HttpSession session) {
        return cartService.getCart(session);
    }

    @ModelAttribute("navCategories")
    public List<TourCategory> navCategories() {
        return categoryService.findActiveCategories();
    }
}
