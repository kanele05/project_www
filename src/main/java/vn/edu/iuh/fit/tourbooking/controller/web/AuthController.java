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

@Controller
@RequiredArgsConstructor
// Đăng nhập/đăng ký: trang đăng nhập do Spring Security xử lý, controller lo phần đăng ký.
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

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult binding,
                           RedirectAttributes ra) {
        if (binding.hasErrors()) {
            return "auth/register";
        }

        userService.register(form);

        ra.addFlashAttribute("successMessage", "registered");
        return "redirect:/login?registered";
    }
}
