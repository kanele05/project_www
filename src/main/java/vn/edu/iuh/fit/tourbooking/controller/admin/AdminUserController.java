package vn.edu.iuh.fit.tourbooking.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.tourbooking.dto.form.AdminUserForm;
import vn.edu.iuh.fit.tourbooking.entity.Role;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;
import vn.edu.iuh.fit.tourbooking.service.BookingService;
import vn.edu.iuh.fit.tourbooking.service.UserService;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
// Khu quản trị: thêm/sửa/xoá/khoá tài khoản người dùng.
public class AdminUserController {

    private final UserService userService;
    private final BookingService bookingService;
    private final MessageHelper messages;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Role role,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("usersPage", userService.adminSearch(keyword, role, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("roles", Role.values());
        model.addAttribute("pageUrlPrefix", buildPageUrl(keyword, role));
        return "admin/user/list";
    }

    @GetMapping("/{id}/view")
    public String view(@PathVariable Long id,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("user", userService.getById(id));
        model.addAttribute("bookingsPage", bookingService.findByUser(id, page));
        return "admin/user/detail";
    }

    @GetMapping("/form")
    public String form(@RequestParam(required = false) Long id, Model model) {
        if (!model.containsAttribute("userForm")) {
            model.addAttribute("userForm", id == null
                    ? new AdminUserForm()
                    : AdminUserForm.from(userService.getById(id)));
        }
        model.addAttribute("roles", Role.values());
        return "admin/user/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("userForm") AdminUserForm form,
                       BindingResult binding,
                       @AuthenticationPrincipal CustomUserDetails principal,
                       Model model,
                       RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "admin/user/form";
        }
        try {
            userService.saveFromAdmin(form, principal.getId());
            ra.addFlashAttribute("successMessage",
                    messages.get(form.isNew() ? "admin.user.created" : "admin.user.updated",
                            form.getEmail()));
            return "redirect:/admin/users";

        } catch (BusinessRuleException e) {

            String field = e.getMessageKey().startsWith("error.user.passwordRequired")
                    ? "newPassword" : "role";
            binding.rejectValue(field, "error", messages.of(e));
            model.addAttribute("roles", Role.values());
            return "admin/user/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes ra) {
        try {
            userService.delete(id, principal.getId());
            ra.addFlashAttribute("successMessage", messages.get("admin.user.deleted"));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", messages.of(e));
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-enabled")
    public String toggleEnabled(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                RedirectAttributes ra) {
        try {
            boolean enabled = userService.toggleEnabled(id, principal.getId());
            ra.addFlashAttribute("successMessage",
                    messages.get(enabled ? "admin.user.enabledMsg" : "admin.user.disabledMsg"));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", messages.of(e));
        }
        return "redirect:/admin/users";
    }

    private String buildPageUrl(String keyword, Role role) {
        StringBuilder sb = new StringBuilder("/admin/users?");
        if (keyword != null && !keyword.isBlank()) {
            sb.append("keyword=").append(java.net.URLEncoder.encode(keyword,
                    java.nio.charset.StandardCharsets.UTF_8)).append('&');
        }
        if (role != null) {
            sb.append("role=").append(role.name()).append('&');
        }
        return sb.toString();
    }
}
