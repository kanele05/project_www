package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.Role;
import vn.edu.iuh.fit.tourbooking.entity.User;

import java.util.Optional;

/**
 * Truy vấn tài khoản người dùng.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Spring Security nạp tài khoản theo email (email đóng vai trò username). */
    Optional<User> findByEmail(String email);

    /** Dùng cho ràng buộc {@code @UniqueEmail} lúc đăng ký. */
    boolean existsByEmail(String email);

    /**
     * Kiểm tra trùng email khi <b>sửa</b> hồ sơ: bỏ qua chính bản ghi đang sửa,
     * nếu không người dùng sẽ không lưu được khi giữ nguyên email của mình.
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Đếm số quản trị viên đang hoạt động - dùng để chặn xoá quản trị viên cuối
     * cùng, tránh tình huống không còn ai vào được khu vực quản trị.
     */
    long countByRoleAndEnabledTrue(Role role);

    /** Tìm kiếm ở màn quản trị: gõ tên, email hoặc số điện thoại đều ra. */
    @Query("""
            SELECT u FROM User u
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR u.fullName LIKE %:keyword%
                   OR u.email    LIKE %:keyword%
                   OR u.phone    LIKE %:keyword%)
              AND (:role IS NULL OR u.role = :role)
            """)
    Page<User> search(@Param("keyword") String keyword,
                      @Param("role") Role role,
                      Pageable pageable);
}
