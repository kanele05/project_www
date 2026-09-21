package vn.edu.iuh.fit.tourbooking.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;
import vn.edu.iuh.fit.tourbooking.service.UserService;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

/**
 * Web service về tài khoản.
 *
 * <p>Chỉ có đúng một địa chỉ, và nó cố tình rất hẹp: kiểm tra <b>một</b> email
 * đã có người dùng chưa. Không có địa chỉ nào liệt kê tài khoản hay tra cứu
 * thông tin người khác - việc đó thuộc khu vực quản trị.</p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;
    private final MessageHelper messages;

    /**
     * @param available {@code true} nếu email còn dùng được
     * @param message   câu để hiện ngay dưới ô nhập, đã theo ngôn ngữ đang chọn
     */
    public record EmailAvailability(String email, boolean available, String message) {
    }

    /**
     * Kiểm tra email trùng ngay lúc người dùng đang gõ.
     *
     * <p><b>Mã tài khoản cần bỏ qua lấy từ phiên đăng nhập chứ không lấy từ tham
     * số.</b> Nếu nhận {@code excludeId} do trình duyệt gửi lên thì ai cũng dò
     * được email nào đang thuộc về tài khoản nào bằng cách thử từng số - còn lấy
     * từ principal thì người dùng chỉ bỏ qua được đúng chính mình.</p>
     *
     * <p>Đây là tiện ích cho người dùng, <b>không phải</b> lớp bảo vệ: quy tắc
     * chặn thật vẫn nằm ở {@code @UniqueEmail} lúc gửi biểu mẫu và ở
     * {@code UserService.register}. Giữa lúc kiểm tra và lúc bấm gửi vẫn có thể
     * có người khác đăng ký mất email đó.</p>
     */
    @GetMapping("/email-available")
    public EmailAvailability emailAvailable(@RequestParam String email,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        Long excludeId = principal == null ? null : principal.getId();
        boolean available = userService.isEmailAvailable(email, excludeId);

        return new EmailAvailability(email, available,
                messages.get(available ? "api.email.available" : "validation.email.duplicate"));
    }
}
