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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một lần mã giảm giá được dùng thật: mã nào, ai dùng, cho đơn nào, giảm bao nhiêu.
 *
 * <p>Không có bảng này thì {@code Promotion.usedCount} chỉ là một con số không ai
 * đối chiếu được, và không cách nào chặn "mỗi người một lượt".</p>
 *
 * <p>Ràng buộc duy nhất trên {@code booking_id}: <b>mỗi đơn chỉ áp một mã</b> - đây
 * là quyết định thiết kế, cộng dồn nhiều mã lên một đơn là nguồn gốc của những hoá
 * đơn không ai giải thích nổi.</p>
 *
 * <p>{@code discountAmount} chép lại số tiền đã giảm tại thời điểm đặt, không tính
 * lại từ {@link Promotion}: quản trị viên sửa giá trị mã về sau thì đơn cũ vẫn
 * phải giữ nguyên số đã giảm - cùng nguyên tắc với các cột bản sao ở
 * {@link BookingDetail}.</p>
 */
@Entity
@Table(
        name = "coupon_usages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_coupon_usages_booking", columnNames = "booking_id"),
        indexes = {
                @Index(name = "idx_coupon_usages_promotion", columnList = "promotion_id"),
                @Index(name = "idx_coupon_usages_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_coupon_usages_promotion"))
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_coupon_usages_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_coupon_usages_booking"))
    private Booking booking;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt = LocalDateTime.now();

    public CouponUsage(Promotion promotion, User user, Booking booking, BigDecimal discountAmount) {
        this.promotion = promotion;
        this.user = user;
        this.booking = booking;
        this.discountAmount = discountAmount;
        this.usedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CouponUsage other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
