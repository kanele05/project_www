package vn.edu.iuh.fit.tourbooking.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import vn.edu.iuh.fit.tourbooking.dto.form.PromotionForm;
import vn.edu.iuh.fit.tourbooking.entity.DiscountType;
import vn.edu.iuh.fit.tourbooking.entity.Promotion;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.service.PromotionService;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
// Khu quản trị: thêm/sửa/xoá/bật-tắt mã khuyến mãi và xem lịch sử lượt dùng.
public class AdminPromotionController {

    private final PromotionService promotionService;
    private final MessageHelper messages;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Boolean active,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("promotionsPage", promotionService.adminSearch(keyword, active, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", active);
        model.addAttribute("pageUrlPrefix", buildPageUrl(keyword, active));
        return "admin/promotion/list";
    }

    @GetMapping("/form")
    public String form(@RequestParam(required = false) Long id, Model model) {
        if (!model.containsAttribute("promotionForm")) {
            model.addAttribute("promotionForm", id == null
                    ? new PromotionForm()
                    : PromotionForm.from(promotionService.getById(id)));
        }
        model.addAttribute("discountTypes", DiscountType.values());
        return "admin/promotion/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("promotionForm") PromotionForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("discountTypes", DiscountType.values());
            return "admin/promotion/form";
        }
        try {
            Promotion saved = promotionService.save(form);
            ra.addFlashAttribute("successMessage",
                    messages.get(form.getId() == null
                            ? "admin.promotion.created" : "admin.promotion.updated", saved.getCode()));
            return "redirect:/admin/promotions";

        } catch (BusinessRuleException e) {
            binding.rejectValue("code", "error", messages.of(e));
            model.addAttribute("discountTypes", DiscountType.values());
            return "admin/promotion/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            promotionService.delete(id);
            ra.addFlashAttribute("successMessage", messages.get("admin.promotion.deleted"));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", messages.of(e));
        }
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes ra) {
        boolean active = promotionService.toggleActive(id);
        ra.addFlashAttribute("successMessage",
                messages.get(active ? "admin.promotion.activated" : "admin.promotion.deactivated"));
        return "redirect:/admin/promotions";
    }

    @GetMapping("/{id}/usages")
    public String usages(@PathVariable Long id,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        model.addAttribute("promotion", promotionService.getById(id));
        model.addAttribute("usagesPage", promotionService.usagesOf(id, page));
        model.addAttribute("pageUrlPrefix", "/admin/promotions/" + id + "/usages?");
        return "admin/promotion/usages";
    }

    private String buildPageUrl(String keyword, Boolean active) {
        StringBuilder sb = new StringBuilder("/admin/promotions?");
        if (keyword != null && !keyword.isBlank()) {
            sb.append("keyword=").append(java.net.URLEncoder.encode(keyword,
                    java.nio.charset.StandardCharsets.UTF_8)).append('&');
        }
        if (active != null) {
            sb.append("active=").append(active).append('&');
        }
        return sb.toString();
    }
}
