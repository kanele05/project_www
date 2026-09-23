package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.edu.iuh.fit.tourbooking.entity.Role;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.validation.UniqueEmail;

@Data
@UniqueEmail(excludeIdField = "id")
// Biểu mẫu thêm/sửa tài khoản ở khu quản trị (mật khẩu để trống khi sửa = giữ nguyên).
public class AdminUserForm {

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

    @NotNull(message = "{validation.user.role.required}")
    private Role role = Role.CUSTOMER;

    private boolean enabled = true;

    @Pattern(regexp = "^$|.{6,50}", message = "{validation.password.size}")
    private String newPassword;

    public boolean isNew() {
        return id == null;
    }

    public static AdminUserForm from(User user) {
        AdminUserForm form = new AdminUserForm();
        form.setId(user.getId());
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        form.setAddress(user.getAddress());
        form.setRole(user.getRole());
        form.setEnabled(user.isEnabled());
        return form;
    }
}
