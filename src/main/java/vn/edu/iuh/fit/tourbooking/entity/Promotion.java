package vn.edu.iuh.fit.tourbooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

// Mã giảm giá: điều kiện thời gian/lượt dùng/đơn tối thiểu và cách tính số tiền giảm.
@Entity
@Table(
        name = "promotions",
        uniqueConstraints = @UniqueConstraint(name = "uk_promotions_code", columnNames = "code"),
        indexes = {
                @Index(name = "idx_promotions_active", columnList = "active"),
                @Index(name = "idx_promotions_period", columnList = "start_at,end_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Promotion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType = DiscountType.PERCENT;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 15, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "min_order_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // Mã còn dùng được không: đang trong thời gian hiệu lực, bật, và còn lượt.
    public boolean isRunning(LocalDateTime now) {
        return active
                && startAt != null && !now.isBefore(startAt)
                && endAt != null && !now.isAfter(endAt)
                && hasQuotaLeft();
    }

    public boolean hasQuotaLeft() {
        return usageLimit == null || usedCount == null || usedCount < usageLimit;
    }

    // Tính số tiền được giảm cho một đơn: kiểm đơn tối thiểu, tính theo % hoặc số tiền cố định, chặn trần.
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(minOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = discountType == DiscountType.PERCENT
                ? orderAmount.multiply(discountValue)
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : discountValue;

        if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        return discount.min(orderAmount);
    }

    public void increaseUsedCount() {
        this.usedCount = (usedCount == null ? 0 : usedCount) + 1;
    }

    public void decreaseUsedCount() {
        this.usedCount = Math.max(0, (usedCount == null ? 0 : usedCount) - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Promotion other)) return false;
        return code != null && code.equals(other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
