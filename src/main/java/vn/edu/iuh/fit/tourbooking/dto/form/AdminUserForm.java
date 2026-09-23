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

/**
 * Biểu mẫu quản lý tài khoản ở khu vực quản trị.
 *
 * <p><b>Điểm quan trọng nhất: biểu mẫu này KHÔNG mang chuỗi băm mật khẩu.</b>
 * {@link #from(User)} chép mọi thứ trừ mật khẩu, nên dù khuôn mẫu có vô tình
 * viết {@code th:field="*{...}"} sai chỗ thì cũng không có gì để lộ. Xem mã
 * nguồn trang trong trình duyệt sẽ không tìm thấy chuỗi {@code $2a$} nào.</p>
 *
 * <p>{@link #newPassword} là ô <b>chỉ ghi</b>: bắt buộc khi tạo tài khoản mới,
 * còn khi sửa thì để trống nghĩa là giữ nguyên mật khẩu cũ. Nó không bao giờ
 * được điền sẵn giá trị.</p>
 */
@Data
@UniqueEmail(excludeIdField = "id")
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

    /**
     * Mật khẩu mới. Ràng buộc độ dài chỉ áp dụng khi có nhập, việc "bắt buộc khi
     * tạo mới" do tầng service kiểm tra vì nó phụ thuộc vào {@code id} có null
     * hay không.
     *
     * <p><b>Cố ý dùng {@code @Pattern} thay vì {@code @Size}.</b> {@code @Size}
     * không coi chuỗi rỗng là hợp lệ theo nghĩa "bỏ qua ràng buộc" - nó vẫn đo độ
     * dài của chuỗi rỗng (0) và so với {@code min=6} nên luôn thất bại. Hậu quả:
     * ô để trống lúc sửa tài khoản (đúng ý "giữ nguyên mật khẩu cũ" ghi ở Javadoc
     * lớp) bị validator chặn ngay từ vòng {@code @Valid}, quản trị viên không sửa
     * được bất cứ gì trên tài khoản nếu không đồng thời đặt lại mật khẩu.
     * {@code ^$|.{6,50}} chấp nhận đúng hai trường hợp: rỗng hẳn, hoặc từ 6-50 ký
     * tự bất kỳ.</p>
     */
    @Pattern(regexp = "^$|.{6,50}", message = "{validation.password.size}")
    private String newPassword;

    public boolean isNew() {
        return id == null;
    }

    /** Đổ dữ liệu tài khoản vào biểu mẫu - cố ý bỏ qua trường mật khẩu. */
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
