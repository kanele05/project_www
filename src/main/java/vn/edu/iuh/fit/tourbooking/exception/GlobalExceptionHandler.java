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

/**
 * Lưới an toàn cho tầng giao diện.
 *
 * <p>Các controller đều đã tự bắt {@link BusinessRuleException} ở những chỗ dự
 * kiến có thể vi phạm quy tắc, để đặt thông báo vào đúng trang tương ứng. Lớp
 * này chỉ đỡ những trường hợp <b>bị bỏ sót</b>: nếu không, một quy tắc nghiệp vụ
 * bị vi phạm ở chỗ chưa lường trước sẽ hiện ra trang lỗi 500, làm người dùng
 * tưởng hệ thống hỏng trong khi thực ra họ chỉ thao tác sai.</p>
 *
 * <p>Phạm vi giới hạn ở hai gói giao diện. Gói {@code controller.api} có
 * {@link ApiExceptionHandler} riêng trả JSON - hai lớp không giẫm chân nhau.</p>
 *
 * <p><b>Cố ý không bắt {@code Exception} chung.</b> Nuốt mọi ngoại lệ rồi chuyển
 * hướng kèm một câu chung chung sẽ giấu luôn các lỗi lập trình thật; những lỗi
 * đó phải đi tới trang 500 và để lại vết trong nhật ký.</p>
 */
@ControllerAdvice(basePackages = {
        "vn.edu.iuh.fit.tourbooking.controller.web",
        "vn.edu.iuh.fit.tourbooking.controller.admin"
})
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageHelper messages;

    @ExceptionHandler(BusinessRuleException.class)
    public String handleBusinessRule(BusinessRuleException e,
                                     HttpServletRequest request,
                                     RedirectAttributes ra) {
        log.warn("Vi phạm quy tắc nghiệp vụ chưa được xử lý tại {}: {}",
                request.getRequestURI(), e.getMessageKey());

        ra.addFlashAttribute("errorMessage", messages.of(e));
        // Đưa người dùng về đúng trang họ vừa thao tác, kèm thông báo - hơn hẳn
        // việc đá về trang chủ và bắt tự tìm lại chỗ cũ.
        return "redirect:" + SafeRedirect.refererPath(request, "/");
    }

    /**
     * Lưới an toàn cho ràng buộc {@code @Min}/{@code @Max}... trên tham số
     * {@code @RequestParam} của các controller có {@code @Validated} ở mức lớp
     * (ví dụ {@code AdminBookingController.updateDetail}). Không có lưới này thì
     * gửi thẳng một giá trị âm sẽ ném {@link ConstraintViolationException} không
     * ai bắt, ra thẳng trang 500 - đúng thứ đề bài không muốn.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolation(ConstraintViolationException e,
                                            HttpServletRequest request,
                                            RedirectAttributes ra) {
        log.warn("Tham số không hợp lệ tại {}: {}", request.getRequestURI(), e.getMessage());

        ra.addFlashAttribute("errorMessage", messages.get("error.request.invalidParam"));
        return "redirect:" + SafeRedirect.refererPath(request, "/");
    }

    /**
     * Lưới an toàn cho khoá lạc quan ({@code @Version}, xem {@code TourDeparture}).
     *
     * <p>Kịch bản thật (1.18): quản trị viên đang sửa một đợt khởi hành đúng lúc có
     * khách đặt tour thành công cho đợt đó (hoặc một quản trị viên khác cũng đang
     * sửa) - phiên bản trong tay quản trị viên đã cũ hơn bản mới nhất dưới CSDL,
     * Hibernate ném {@link ObjectOptimisticLockingFailureException} khi cố ghi đè.
     * Không có lưới này thì người dùng nhận thẳng trang 500 dù họ không thao tác
     * sai gì cả - chỉ là chậm chân hơn người khác một chút. Bắt lại thành một câu
     * thân thiện, mời họ tải lại và thử lại.</p>
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String handleOptimisticLock(ObjectOptimisticLockingFailureException e,
                                       HttpServletRequest request,
                                       RedirectAttributes ra) {
        log.warn("Khoá lạc quan xung đột tại {}: {}", request.getRequestURI(), e.getMessage());

        ra.addFlashAttribute("errorMessage", messages.get("error.request.dataChanged"));
        return "redirect:" + SafeRedirect.refererPath(request, "/");
    }
}
