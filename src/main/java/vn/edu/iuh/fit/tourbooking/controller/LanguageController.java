package vn.edu.iuh.fit.tourbooking.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.iuh.fit.tourbooking.util.SafeRedirect;

/**
 * Đổi ngôn ngữ rồi quay lại đúng trang người dùng đang đọc dở.
 *
 * <p>Việc <i>đổi</i> ngôn ngữ không nằm ở đây: {@code LocaleChangeInterceptor}
 * đã đọc tham số {@code ?lang=} và {@code CookieLocaleResolver} đã ghi cookie
 * trước khi phương thức này chạy. Nhiệm vụ duy nhất còn lại là chuyển hướng về
 * chỗ cũ - và đó cũng chính là lý do phải có một địa chỉ riêng thay vì chỉ gắn
 * {@code ?lang=en} vào liên kết hiện tại: gắn thẳng như vậy rất dễ làm
 * <b>mất bộ lọc và số trang</b> đang có trên địa chỉ, còn ở đây tham số truy vấn
 * được giữ nguyên từng chữ (xem {@link SafeRedirect}).</p>
 *
 * <p>Đặt ngoài gói {@code controller.web} là có chủ ý: {@code GlobalModelAdvice}
 * chỉ áp dụng cho gói đó, nên một lần chuyển hướng không phải kéo theo hai câu
 * truy vấn giỏ hàng và danh mục vô ích.</p>
 */
@Controller
public class LanguageController {

    @GetMapping("/change-language")
    public String changeLanguage(HttpServletRequest request) {
        return "redirect:" + SafeRedirect.refererPath(request, "/");
    }
}
