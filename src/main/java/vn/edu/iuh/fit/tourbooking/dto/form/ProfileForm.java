package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.edu.iuh.fit.tourbooking.validation.UniqueEmail;

/**
 * Biểu mẫu sửa thông tin cá nhân.
 *
 * <p>Cố ý <b>không có trường mật khẩu</b>: đổi mật khẩu là một luồng riêng, có
 * bước xác nhận mật khẩu hiện tại. Không có trường thì cũng không thể vô tình
 * làm lộ chuỗi băm ra HTML qua {@code th:field}.</p>
 *
 * <p>{@code excludeIdField = "id"} để khi người dùng giữ nguyên email của chính
 * mình thì không bị báo trùng.</p>
 */
@Data
@UniqueEmail(excludeIdField = "id")
public class ProfileForm {

    /** Mã người dùng đang sửa - lấy từ tài khoản đang đăng nhập, không nhận từ trình duyệt. */
    private Long id;

    @NotBlank(message = "{validation.fullName.required}")
    @Size(max = 100, message = "{validation.fullName.size}")
    private String fullName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 150, message = "{validation.email.size}")
    private String email;

    @NotBlank(message = "{validation.phone.required}")
    @Pattern(regexp = "^0\\d{9,10}$", message = "{validation.phone.invalid}")
    private String phone;

    @Size(max = 255, message = "{validation.address.size}")
    private String address;
}
