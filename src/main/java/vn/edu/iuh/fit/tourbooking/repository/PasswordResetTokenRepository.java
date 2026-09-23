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

@Repository
// Truy vấn vé đặt lại mật khẩu.
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :time")
    int deleteExpired(@Param("time") LocalDateTime time);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
