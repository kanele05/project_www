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

/**
 * Quản lý tài khoản người dùng.
 *
 * <p>Biểu mẫu bind vào {@code AdminUserForm} - lớp này <b>không hề có trường
 * chứa chuỗi băm mật khẩu</b>, nên không có đường nào để mật khẩu lọt ra mã
 * nguồn trang. Xem mã nguồn trang danh sách hay trang sửa đều không tìm thấy
 * chuỗi {@code $2a$} nào.</p>
 */
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
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

    /**
     * Trang chi tiết CHỈ XEM (mục 12.8 - đề bài đòi "xem chi tiết từng tài
     * khoản người dùng, không xem được password"). {@code AdminUserForm} không
     * hề mang chuỗi băm mật khẩu (xem Javadoc lớp), nhưng để chắc chắn hơn nữa,
     * trang này đọc thẳng từ {@code User} và HTML sinh ra <b>không có ô nhập
     * nào</b> - không có nơi nào để lỡ tay in mật khẩu ra.
     */
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
            // Lỗi mật khẩu gắn vào đúng ô đó; lỗi "tự hạ quyền / khoá chính mình"
            // hay "hạ quyền admin cuối cùng" gắn vào ô vai trò, vì đó là trường
            // người dùng cần sửa lại để đi tiếp.
            String field = e.getMessageKey().startsWith("error.user.passwordRequired")
                    ? "newPassword" : "role";
            binding.rejectValue(field, "error", messages.of(e));
            model.addAttribute("roles", Role.values());
            return "admin/user/form";
        }
    }

    /**
     * Xoá tài khoản - bị chặn trong ba trường hợp: tự xoá chính mình, xoá quản
     * trị viên cuối cùng, hoặc tài khoản đã từng đặt tour.
     */
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
