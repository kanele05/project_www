package vn.edu.iuh.fit.tourbooking.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.tourbooking.dto.form.ContactForm;
import vn.edu.iuh.fit.tourbooking.service.ContactService;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

/**
 * Gửi liên hệ công khai (UC021) - {@code AC01 Khách vãng lai}, không cần đăng nhập.
 */
@Controller
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final MessageHelper messages;

    /**
     * @param tourId có giá trị khi mở từ nút "Liên hệ về tour này" ở trang chi
     *               tiết tour, điền sẵn cả tour lẫn ô ẩn {@code tourId}.
     */
    @GetMapping("/contact")
    public String form(@RequestParam(required = false) Long tourId, Model model) {
        if (!model.containsAttribute("contactForm")) {
            ContactForm form = new ContactForm();
            form.setTourId(tourId);
            model.addAttribute("contactForm", form);
        }
        model.addAttribute("prefillTour", contactService.findTourForPrefill(tourId));
        return "contact";
    }

    @PostMapping("/contact")
    public String submit(@Valid @ModelAttribute("contactForm") ContactForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("prefillTour", contactService.findTourForPrefill(form.getTourId()));
            return "contact";
        }
        contactService.submit(form);
        ra.addFlashAttribute("successMessage", messages.get("contact.submitted"));
        return "redirect:/contact";
    }
}
