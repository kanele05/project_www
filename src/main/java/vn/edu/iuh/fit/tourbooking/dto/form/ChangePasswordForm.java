package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.edu.iuh.fit.tourbooking.validation.PasswordsMatch;

@Data
@PasswordsMatch(passwordField = "newPassword", confirmField = "confirmPassword")
// Biểu mẫu đổi mật khẩu ở trang tài khoản cá nhân.
public class ChangePasswordForm {

    @NotBlank(message = "{validation.currentPassword.required}")
    private String currentPassword;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 6, max = 50, message = "{validation.password.size}")
    private String newPassword;

    @NotBlank(message = "{validation.confirmPassword.required}")
    private String confirmPassword;
}
