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
import vn.edu.iuh.fit.tourbooking.dto.form.DepartureForm;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.service.DepartureService;
import vn.edu.iuh.fit.tourbooking.service.TourService;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

@Controller
@RequestMapping("/admin/tours/{tourId}/departures")
@RequiredArgsConstructor
// Khu quản trị: thêm/sửa/xoá/bật-tắt lịch khởi hành của một tour.
public class AdminDepartureController {

    private final DepartureService departureService;
    private final TourService tourService;
    private final MessageHelper messages;

    @GetMapping
    public String list(@PathVariable Long tourId,
                       @RequestParam(required = false) Long editId,
                       Model model) {
        model.addAttribute("tour", tourService.getForEdit(tourId));
        model.addAttribute("departures", departureService.findByTour(tourId));

        if (!model.containsAttribute("departureForm")) {
            model.addAttribute("departureForm", editId == null
                    ? new DepartureForm()
                    : DepartureForm.from(departureService.getOfTour(tourId, editId)));
        }
        return "admin/departure/list";
    }

    @PostMapping("/save")
    public String save(@PathVariable Long tourId,
                       @Valid @ModelAttribute("departureForm") DepartureForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes ra) {
        if (binding.hasErrors()) {
            return backToList(tourId, model);
        }
        try {
            departureService.save(tourId, form);
            ra.addFlashAttribute("successMessage",
                    messages.get(form.getId() == null
                            ? "admin.departure.created" : "admin.departure.updated"));
            return "redirect:/admin/tours/" + tourId + "/departures";

        } catch (BusinessRuleException e) {
            binding.rejectValue("departureDate", "error", messages.of(e));
            return backToList(tourId, model);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long tourId, @PathVariable Long id,
                         RedirectAttributes ra) {
        try {
            departureService.delete(tourId, id);
            ra.addFlashAttribute("successMessage", messages.get("admin.departure.deleted"));
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("errorMessage", messages.of(e));
        }
        return "redirect:/admin/tours/" + tourId + "/departures";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long tourId, @PathVariable Long id,
                               RedirectAttributes ra) {
        boolean active = departureService.toggleActive(tourId, id);
        ra.addFlashAttribute("successMessage",
                messages.get(active ? "admin.departure.opened" : "admin.departure.closed"));
        return "redirect:/admin/tours/" + tourId + "/departures";
    }

    private String backToList(Long tourId, Model model) {
        model.addAttribute("tour", tourService.getForEdit(tourId));
        model.addAttribute("departures", departureService.findByTour(tourId));
        return "admin/departure/list";
    }
}
