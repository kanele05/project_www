package vn.edu.iuh.fit.tourbooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Vé một lần dùng để đặt lại mật khẩu.
 *
 * <p>Ba quy tắc an toàn nằm trong bảng này chứ không nằm trong ràng buộc CSDL
 * (đề bài cấm CHECK / Trigger), tầng service kiểm tra khi khách bấm vào liên kết:
 * vé phải <b>chưa dùng</b>, <b>chưa hết hạn</b>, và mỗi lần yêu cầu mới sẽ vô hiệu
 * hoá các vé cũ của cùng tài khoản.</p>
 *
 * <p>Cột {@code token} lưu chuỗi ngẫu nhiên dài, không lưu email trong liên kết:
 * đoán được token là đổi được mật khẩu, nên nó phải khó đoán và có hạn dùng.</p>
 */
@Entity
@Table(
        name = "password_reset_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_password_reset_tokens_token", columnNames = "token"),
        indexes = @Index(name = "idx_password_reset_tokens_user", columnList = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken extends Auditable {

    /** Thời hạn mặc định của một vé, tính bằng phút. */
    public static final long VALID_MINUTES = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_password_reset_tokens_user"))
    private User user;

    @Column(name = "token", nullable = false, length = 100)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Null nghĩa là vé chưa được dùng. Dùng rồi thì không dùng lại được nữa. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public PasswordResetToken(User user, String token, LocalDateTime expiresAt) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt == null || expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    /** Vé chỉ dùng được khi vừa chưa hết hạn vừa chưa bị dùng. */
    public boolean isUsable() {
        return !isUsed() && !isExpired();
    }

    public void markUsed() {
        this.usedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasswordResetToken other)) return false;
        return token != null && token.equals(other.token);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(token);
    }
}
