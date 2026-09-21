package vn.edu.iuh.fit.tourbooking.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.edu.iuh.fit.tourbooking.entity.Role;
import vn.edu.iuh.fit.tourbooking.entity.User;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * Bọc {@link User} lại cho Spring Security dùng.
 *
 * <p>Ngoài những gì {@link UserDetails} bắt buộc, lớp này còn giữ thêm
 * {@code id} và {@code fullName}. Nhờ vậy khi cần kiểm tra quyền sở hữu đơn hàng
 * chỉ việc so {@code booking.user.id} với {@code principal.id}, không phải truy
 * vấn CSDL thêm một lần chỉ để đổi email lấy id.</p>
 *
 * <p><b>Không</b> giữ tham chiếu tới chính đối tượng {@code User}: nó là entity
 * JPA, mà đối tượng này sống trong session suốt phiên đăng nhập - giữ cả entity
 * sẽ kéo theo một bản sao dữ liệu cũ (kể cả chuỗi băm mật khẩu) nằm lì trong bộ
 * nhớ và có nguy cơ bị ghi ra đĩa cùng session.</p>
 */
@Getter
public class CustomUserDetails implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final String fullName;
    private final Role role;

    private final String password;
    private final boolean enabled;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.password = user.getPassword();
        this.enabled = user.isEnabled();
    }

    /**
     * Spring Security quy ước tên quyền phải bắt đầu bằng {@code ROLE_} thì
     * {@code hasRole("ADMIN")} mới khớp - {@link Role#authority()} lo phần đó.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    /** Đăng nhập bằng email nên email đóng luôn vai trò tên đăng nhập. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Tài khoản bị quản trị viên vô hiệu hoá thì không đăng nhập được. */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
