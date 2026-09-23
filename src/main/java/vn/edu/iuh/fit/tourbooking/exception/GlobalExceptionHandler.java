package vn.edu.iuh.fit.tourbooking.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;
import vn.edu.iuh.fit.tourbooking.util.SafeRedirect;

@ControllerAdvice(basePackages = {
        "vn.edu.iuh.fit.tourbooking.controller.web",
        "vn.edu.iuh.fit.tourbooking.controller.admin"
})
@RequiredArgsConstructor
@Slf4j
// Bắt lỗi cho controller web/admin: đổi lỗi thành flash message + redirect (PRG) thay vì trang 500 thô.
public class GlobalExceptionHandler {

    private final MessageHelper messages;

    @ExceptionHandler(BusinessRuleException.class)
    public String handleBusinessRule(BusinessRuleException e,
                                     HttpServletRequest request,
                                     RedirectAttributes ra) {
        log.warn("Vi phạm quy tắc nghiệp vụ chưa được xử lý tại {}: {}",
                request.getRequestURI(), e.getMessageKey());

        ra.addFlashAttribute("errorMessage", messages.of(e));

        return "redirect:" + SafeRedirect.refererPath(request, "/");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolation(ConstraintViolationException e,
                                            HttpServletRequest request,
                                            RedirectAttributes ra) {
        log.warn("Tham số không hợp lệ tại {}: {}", request.getRequestURI(), e.getMessage());

        ra.addFlashAttribute("errorMessage", messages.get("error.request.invalidParam"));
        return "redirect:" + SafeRedirect.refererPath(request, "/");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String handleOptimisticLock(ObjectOptimisticLockingFailureException e,
                                       HttpServletRequest request,
                                       RedirectAttributes ra) {
        log.warn("Khoá lạc quan xung đột tại {}: {}", request.getRequestURI(), e.getMessage());

        ra.addFlashAttribute("errorMessage", messages.get("error.request.dataChanged"));
        return "redirect:" + SafeRedirect.refererPath(request, "/");
    }
}
