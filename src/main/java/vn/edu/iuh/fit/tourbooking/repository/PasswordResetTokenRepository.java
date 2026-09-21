package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Truy vấn vé đặt lại mật khẩu. */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /** Các vé còn hiệu lực của một tài khoản - dùng để vô hiệu hoá khi cấp vé mới. */
    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId);

    long countByUserId(Long userId);

    /** Dọn vé quá hạn. Gọi từ service có {@code @Transactional}. */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :time")
    int deleteExpired(@Param("time") LocalDateTime time);

    /**
     * Xoá hết vé của một tài khoản sắp bị xoá. Vé đặt lại mật khẩu không có giá
     * trị lưu trữ, nên đây là dữ liệu duy nhất được xoá theo người dùng thay vì
     * chặn việc xoá.
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
