package vn.edu.iuh.fit.tourbooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Tài khoản người dùng - dùng chung cho cả khách hàng lẫn quản trị viên,
 * phân biệt bằng {@link Role}.
 *
 * <p>Tên bảng là {@code users} chứ không phải {@code user} vì {@code USER} là
 * từ khoá dành riêng của SQL Server, để nguyên sẽ lỗi cú pháp khi Hibernate
 * sinh câu lệnh.</p>
 *
 * <p><b>Quy ước Lombok cho entity:</b> chỉ dùng {@code @Getter @Setter}
 * và hai constructor. Tuyệt đối không dùng {@code @Data} / {@code @ToString} /
 * {@code @EqualsAndHashCode} vì chúng duyệt qua mọi association và gây
 * {@code StackOverflowError} ở các quan hệ hai chiều.</p>
 *
 * <p>Chủ ý <b>không</b> map {@code List<Booking>} ở đây: một khách hàng có thể
 * có rất nhiều đơn, nạp hết ra bộ nhớ là vô nghĩa. Cần lịch sử đặt tour thì
 * gọi {@code bookingRepository.findByUserId(id, Pageable)} để phân trang.</p>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        indexes = @Index(name = "idx_users_role", columnList = "role")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Đăng nhập bằng email nên đây vừa là khoá nghiệp vụ, vừa là username. */
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /**
     * Chuỗi băm BCrypt (60 ký tự, bắt đầu bằng {@code $2a$}).
     * Không bao giờ chứa mật khẩu thô. Các DTO gửi ra view đều không có trường này.
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    /**
     * Cố ý <b>không</b> dùng {@code @Enumerated(EnumType.STRING)}: Hibernate 6.2+
     * sẽ tự sinh kèm một ràng buộc CHECK liệt kê các vai trò hợp lệ, mà đề bài
     * thì cấm dùng CHECK constraint. Việc quy đổi do
     * {@code RoleConverter} (đăng ký {@code autoApply}) đảm nhiệm, nhờ vậy
     * Hibernate chỉ thấy một cột chuỗi bình thường.
     *
     * <p>Cũng cố ý <b>không</b> ép {@code columnDefinition = "VARCHAR(20)"}: cấu
     * hình {@code use_nationalized_character_data} khiến Hibernate đọc mọi cột
     * chuỗi bằng {@code getNString()}, mà trình điều khiển SQL Server từ chối
     * đọc một cột VARCHAR theo kiểu đó ("The conversion from varchar to NCHAR is
     * unsupported"). Cột này để mặc định là NVARCHAR như mọi cột chữ khác.</p>
     */
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    /** Cho phép "vô hiệu hoá" tài khoản thay vì xoá khi tài khoản đã có đơn đặt. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public User(String fullName, String email, String password, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * So sánh theo khoá nghiệp vụ ({@code email}) chứ không theo {@code id}:
     * đối tượng vừa {@code new} còn chưa có id, nếu so theo id thì hai bản ghi
     * khác nhau đều {@code null} sẽ bị coi là bằng nhau.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return email != null && email.equalsIgnoreCase(other.email);
    }

    @Override
    public int hashCode() {
        return email == null ? 0 : Objects.hashCode(email.toLowerCase());
    }
}
