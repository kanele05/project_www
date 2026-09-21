package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.edu.iuh.fit.tourbooking.validation.PasswordsMatch;

/**
 * Biểu mẫu đổi mật khẩu.
 *
 * <p>Bắt nhập lại mật khẩu hiện tại: nếu ai đó ngồi vào máy đang mở sẵn phiên
 * đăng nhập, họ vẫn không chiếm được tài khoản.</p>
 */
@Data
@PasswordsMatch(passwordField = "newPassword", confirmField = "confirmPassword")
public class ChangePasswordForm {

    @NotBlank(message = "{validation.currentPassword.required}")
    private String currentPassword;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 6, max = 50, message = "{validation.password.size}")
    private String newPassword;

    @NotBlank(message = "{validation.confirmPassword.required}")
    private String confirmPassword;
}
