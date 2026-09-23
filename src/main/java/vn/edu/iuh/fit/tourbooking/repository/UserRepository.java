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

@Repository
// Truy vấn tài khoản người dùng.
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    long countByRoleAndEnabledTrue(Role role);

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
