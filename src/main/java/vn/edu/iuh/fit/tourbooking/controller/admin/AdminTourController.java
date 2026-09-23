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
import vn.edu.iuh.fit.tourbooking.dto.form.TourForm;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.service.CategoryService;
import vn.edu.iuh.fit.tourbooking.service.TourService;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

@Controller
@RequestMapping("/admin/tours")
@RequiredArgsConstructor
// Khu quản trị: thêm/sửa/xoá/bật-tắt tour, quản lý ảnh đại diện và thư viện ảnh.
public class AdminTourController {

    private final TourService tourService;
    private final CategoryService categoryService;
    private final MessageHelper messages;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("toursPage", tourService.adminSearch(keyword, categoryId, page));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("pageUrlPrefix", buildPageUrl(keyword, categoryId));
        return "admin/tour/list";
    }

    @GetMapping("/{id}/view")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("detail", tourService.adminDetail(id));
        return "admin/tour/detail";
    }

    @GetMapping("/form")
    public String form(@RequestParam(required = false) Long id, Model model) {
        if (!model.containsAttribute("tourForm")) {
            model.addAttribute("tourForm",
                    id == null ? new TourForm() : TourForm.from(tourService.getForEdit(id)));
        }
        if (id != null) {

            model.addAttribute("tour", tourService.getForEdit(id));
        }
        model.addAttribute("categories", categoryService.findAll());
        return "admin/tour/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("tourForm") TourForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes ra) {
        if (binding.hasErrors()) {
            return backToForm(form, model);
        }
        try {
            Tour saved = tourService.save(form);
            ra.addFlashAttribute("successMessage",
                    messages.get(form.getId() == null ? "admin.tour.created" : "admin.tour.updated",
                            saved.getName()));
            return "redirect:/admin/tours";

        } catch (BusinessRuleException e) {

            if (e.getMessageKey() != null && e.getMessageKey().startsWith("error.upload.")) {
                binding.rejectValue("thumbnailFile", "error", messages.of(e));
            } else {
                binding.rejectValue("code", "error", messages.of(e));
            }
            return backToForm(form, model);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            tourService.delete(id);
            ra.addFlashAttribute("successMessage", messages.get("admin.tour.deleted"));
        } catch (BusinessRuleException e) {

            ra.addFlashAttribute("errorMessage", messages.of(e));
        }
        return "redirect:/admin/tours";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes ra) {
        boolean active = tourService.toggleActive(id);
        ra.addFlashAttribute("successMessage",
                messages.get(active ? "admin.tour.activated" : "admin.tour.deactivated"));
        return "redirect:/admin/tours";
    }

    @PostMapping("/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable Long id, @PathVariable Long imageId,
                              RedirectAttributes ra) {
        tourService.deleteImage(id, imageId);
        ra.addFlashAttribute("successMessage", messages.get("admin.tour.imageDeleted"));
        return "redirect:/admin/tours/form?id=" + id;
    }

    private String backToForm(TourForm form, Model model) {
        model.addAttribute("categories", categoryService.findAll());
        if (form.getId() != null) {
            model.addAttribute("tour", tourService.getForEdit(form.getId()));
        }
        return "admin/tour/form";
    }

    private String buildPageUrl(String keyword, Long categoryId) {
        StringBuilder sb = new StringBuilder("/admin/tours?");
        if (keyword != null && !keyword.isBlank()) {
            sb.append("keyword=").append(java.net.URLEncoder.encode(keyword,
                    java.nio.charset.StandardCharsets.UTF_8)).append('&');
        }
        if (categoryId != null) {
            sb.append("categoryId=").append(categoryId).append('&');
        }
        return sb.toString();
    }
}
