package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.edu.iuh.fit.tourbooking.validation.PasswordsMatch;
import vn.edu.iuh.fit.tourbooking.validation.UniqueEmail;

/**
 * Biểu mẫu đăng ký tài khoản.
 *
 * <p><b>Vì sao dùng biểu mẫu riêng chứ không bind thẳng vào entity {@code User}:</b>
 * entity có các trường {@code role} và {@code enabled}. Nếu bind thẳng, người
 * dùng chỉ cần thêm {@code role=ADMIN} vào dữ liệu gửi lên là tự phong mình làm
 * quản trị viên. Biểu mẫu chỉ chứa đúng những gì người dùng được phép nhập.</p>
 *
 * <p>Mọi thông điệp lỗi đều là khoá tra trong {@code messages.properties} (nhờ
 * {@code ValidationConfig} nối Bean Validation với {@code MessageSource}), nên
 * khi thêm tiếng Anh không phải sửa lại lớp này.</p>
 */
@Data
@UniqueEmail
@PasswordsMatch
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

    /** Số điện thoại Việt Nam: bắt đầu bằng 0, tổng cộng 10 hoặc 11 chữ số. */
    @NotBlank(message = "{validation.phone.required}")
    @Pattern(regexp = "^0\\d{9,10}$", message = "{validation.phone.invalid}")
    private String phone;

    @Size(max = 255, message = "{validation.address.size}")
    private String address;
}
