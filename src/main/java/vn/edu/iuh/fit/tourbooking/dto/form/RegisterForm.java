package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.edu.iuh.fit.tourbooking.validation.PasswordsMatch;
import vn.edu.iuh.fit.tourbooking.validation.UniqueEmail;

@Data
@UniqueEmail
@PasswordsMatch
// Biểu mẫu đăng ký tài khoản mới.
public class RegisterForm {

    @NotBlank(message = "{validation.fullName.required}")
    @Size(max = 100, message = "{validation.fullName.size}")
    private String fullName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 150, message = "{validation.email.size}")
    private String email;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 6, max = 50, message = "{validation.password.size}")
    private String password;

    @NotBlank(message = "{validation.confirmPassword.required}")
    private String confirmPassword;

    @NotBlank(message = "{validation.phone.required}")
    @Pattern(regexp = "^0\\d{9,10}$", message = "{validation.phone.invalid}")
    private String phone;

    @Size(max = 255, message = "{validation.address.size}")
    private String address;
}
