package vn.edu.iuh.fit.tourbooking.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.iuh.fit.tourbooking.util.SafeRedirect;

@Controller
// Đổi ngôn ngữ hiển thị rồi quay lại đúng trang trước đó (chỉ chấp nhận Referer cùng miền).
public class LanguageController {

    @GetMapping("/change-language")
    public String changeLanguage(HttpServletRequest request) {
        return "redirect:" + SafeRedirect.refererPath(request, "/");
    }
}
