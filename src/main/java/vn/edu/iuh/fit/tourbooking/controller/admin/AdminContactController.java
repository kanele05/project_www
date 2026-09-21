package vn.edu.iuh.fit.tourbooking.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.tourbooking.entity.ContactStatus;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;
import vn.edu.iuh.fit.tourbooking.service.ContactService;
import vn.edu.iuh.fit.tourbooking.service.UserService;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Quản lý liên hệ (UC022).
 *
 * <p>Cùng khuôn "gấp mở tại dòng" với {@code AdminReviewController}: xem chi
 * tiết và trả lời nằm ngay trong bảng, không cần trang riêng.</p>
 */
@Controller
@RequestMapping("/admin/contacts")
@RequiredArgsConstructor
public class AdminContactController {

    private final ContactService contactService;
    private final UserService userService;
    private final MessageHelper messages;

    /**
     * Mặc định mở ở "Mới" (UC022) - đây là hàng đợi việc phải làm.
     *
     * <p>{@code status="ALL"} nghĩa là không lọc; tách khỏi {@link ContactStatus}
     * vì enum đó không có giá trị "tất cả" và không nên thêm một giá trị giả chỉ
     * để phục vụ bộ lọc giao diện.</p>
     */
    @GetMapping
    public String list(@RequestParam(defaultValue = "NEW") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        // ContactStatus.valueOf(status) ném IllegalArgumentException với một giá
        // trị lạ (?status=xyz), mà GlobalExceptionHandler chỉ bắt
        // BusinessRuleException - trước bản vá này địa chỉ này ra thẳng trang 500.
        // Giá trị lạ thì coi như "ALL" (không lọc), cùng khuôn "vào sai thì về
        // trạng thái mặc định an toàn" như những bộ lọc khác của khu quản trị.
        ContactStatus filter = resolveFilter(status);
        String normalizedStatus = filter == null ? "ALL" : filter.name();
        model.addAttribute("contactsPage", contactService.adminList(filter, page));
        model.addAttribute("status", normalizedStatus);
        model.addAttribute("statuses", ContactStatus.values());
        model.addAttribute("pageUrlPrefix", "/admin/contacts?status=" + normalizedStatus + "&");
        return "admin/contact/list";
    }

    private ContactStatus resolveFilter(String status) {
        if (status == null || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        try {
            return ContactStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam ContactStatus newStatus,
                               @RequestParam(required = false) String replyNote,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes ra) {
        contactService.updateStatus(id, newStatus, userService.getById(principal.getId()), replyNote);
        ra.addFlashAttribute("successMessage", messages.get("admin.contact.updated"));
        return "redirect:/admin/contacts";
    }
}
