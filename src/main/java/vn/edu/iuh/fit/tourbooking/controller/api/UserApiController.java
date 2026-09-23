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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
// REST: kiểm email còn dùng được không (dùng khi validate biểu mẫu đăng ký/hồ sơ bằng AJAX).
public class UserApiController {

    private final UserService userService;
    private final MessageHelper messages;

    public record EmailAvailability(String email, boolean available, String message) {
    }

    @GetMapping("/email-available")
    public EmailAvailability emailAvailable(@RequestParam String email,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        Long excludeId = principal == null ? null : principal.getId();
        boolean available = userService.isEmailAvailable(email, excludeId);

        return new EmailAvailability(email, available,
                messages.get(available ? "api.email.available" : "validation.email.duplicate"));
    }
}
