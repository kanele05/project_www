package vn.edu.iuh.fit.tourbooking.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.tourbooking.dto.form.RegisterForm;
import vn.edu.iuh.fit.tourbooking.service.UserService;

/**
 * Đăng nhập và đăng ký.
 *
 * <p>Việc kiểm tra mật khẩu do Spring Security lo (bộ lọc chặn
 * {@code POST /login} trước khi tới controller), nên ở đây chỉ có
 * {@code GET /login} để hiển thị biểu mẫu.</p>
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    /**
     * Xử lý đăng ký.
     *
     * <p>{@code @Valid} chạy toàn bộ ràng buộc của {@link RegisterForm}, kể cả
     * {@code @UniqueEmail} và {@code @PasswordsMatch}. Có lỗi thì trả về chính
     * trang đăng ký kèm {@code BindingResult} để Thymeleaf tô đỏ đúng ô sai và
     * giữ lại những gì người dùng đã nhập - <b>không</b> chuyển hướng, vì chuyển
     * hướng sẽ làm mất sạch dữ liệu vừa gõ.</p>
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult binding,
                           RedirectAttributes ra) {
        if (binding.hasErrors()) {
            return "auth/register";
        }

        userService.register(form);

        // Đăng ký xong thì chuyển hướng (PRG) để F5 không tạo tài khoản thứ hai.
        ra.addFlashAttribute("successMessage", "registered");
        return "redirect:/login?registered";
    }
}
