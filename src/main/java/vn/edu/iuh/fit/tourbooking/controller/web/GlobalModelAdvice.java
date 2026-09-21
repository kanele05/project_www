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

/**
 * Bơm sẵn hai thứ mà <i>mọi</i> trang công khai đều cần: giỏ hàng (để hiện huy
 * hiệu số dòng trên thanh điều hướng) và danh sách danh mục (để đổ vào menu).
 *
 * <p>Không có lớp này thì mỗi phương thức của mỗi controller đều phải lặp lại hai
 * dòng {@code model.addAttribute(...)}, và chỉ cần quên một chỗ là thanh điều
 * hướng của trang đó trống trơn.</p>
 *
 * <p>Giới hạn phạm vi bằng {@code basePackages}: chỉ áp dụng cho các controller
 * trong gói {@code controller.web}. Khu vực quản trị và các web service REST có
 * nhu cầu khác, không việc gì phải gánh thêm hai truy vấn này.</p>
 */
@ControllerAdvice(basePackages = "vn.edu.iuh.fit.tourbooking.controller.web")
@RequiredArgsConstructor
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
