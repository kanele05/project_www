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
import vn.edu.iuh.fit.tourbooking.dto.form.CategoryForm;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.service.CategoryService;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
// Khu quản trị: thêm/sửa/xoá/bật-tắt danh mục tour.
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final MessageHelper messages;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("categoriesPage", categoryService.adminSearch(keyword, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageUrlPrefix", keyword == null || keyword.isBlank()
                ? "/admin/categories?"
                : "/admin/categories?keyword=" + java.net.URLEncoder.encode(keyword,
                        java.nio.charset.StandardCharsets.UTF_8) + "&");
        return "admin/category/list";
    }

    @GetMapping("/{id}/view")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("detail", categoryService.adminDetail(id));
        return "admin/category/detail";
    }

    @GetMapping("/form")
    public String form(@RequestParam(required = false) Long id, Model model) {
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", id == null
                    ? new CategoryForm()
                    : CategoryForm.from(categoryService.getById(id)));
        }
        return "admin/category/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("categoryForm") CategoryForm form,
                       BindingResult binding,
                       RedirectAttributes ra) {
        if (binding.hasErrors()) {
            return "admin/category/form";
        }
        try {
            TourCategory saved = categoryService.save(form);
            ra.addFlashAttribute("successMessage",
                    messages.get(form.getId() == null
                            ? "admin.category.created" : "admin.category.updated", saved.getName()));
            return "redirect:/admin/categories";

        } catch (BusinessRuleException e) {
            binding.rejectValue("name", "error", messages.of(e));
            return "admin/category/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoryService.delete(id);
            ra.addFlashAttribute("successMessage", messages.get("admin.category.deleted"));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", messages.of(e));
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes ra) {
        boolean active = categoryService.toggleActive(id);
        ra.addFlashAttribute("successMessage",
                messages.get(active ? "admin.category.activated" : "admin.category.deactivated"));
        return "redirect:/admin/categories";
    }
}
