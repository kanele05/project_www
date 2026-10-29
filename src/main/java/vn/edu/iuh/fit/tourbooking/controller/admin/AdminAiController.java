package vn.edu.iuh.fit.tourbooking.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.tourbooking.ai.TourVectorService;
import vn.edu.iuh.fit.tourbooking.config.AiProperties;
import vn.edu.iuh.fit.tourbooking.exception.AiUnavailableException;

@Controller
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
// Trang trạng thái AI cho quản trị viên: nhà cung cấp, mô hình, số tour đã lập chỉ mục và nút lập chỉ mục lại (POST -> redirect, PRG).
public class AdminAiController {

    private final TourVectorService vectorService;
    private final AiProperties properties;
    private final MessageSource messageSource;

    @GetMapping
    public String status(Model model) {
        model.addAttribute("baseUrl", properties.baseUrl());
        model.addAttribute("chatModel", properties.chatModel());
        model.addAttribute("embeddingModel", properties.embeddingModel());
        model.addAttribute("keyConfigured", org.springframework.util.StringUtils.hasText(properties.apiKey()));
        model.addAttribute("threshold", properties.similarityThreshold());
        model.addAttribute("indexStatus", vectorService.status());
        return "admin/ai";
    }

    @PostMapping("/reindex")
    public String reindex(RedirectAttributes ra) {
        try {
            TourVectorService.IndexResult result = vectorService.reindexAll();
            ra.addFlashAttribute("successMessage", messageSource.getMessage("admin.ai.reindex.done",
                    new Object[]{result.indexed(), result.removed()}, LocaleContextHolder.getLocale()));
        } catch (AiUnavailableException e) {
            ra.addFlashAttribute("errorMessage", messageSource.getMessage(e.getMessageKey(), null,
                    LocaleContextHolder.getLocale()));
        }
        return "redirect:/admin/ai";
    }
}
