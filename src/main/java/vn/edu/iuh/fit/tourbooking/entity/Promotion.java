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

/**
 * Chương trình khuyến mãi / mã giảm giá.
 *
 * <p>Toàn bộ điều kiện áp mã đều nằm ở đây và được kiểm bằng Java
 * ({@link #isRunning(LocalDateTime)}, {@link #calculateDiscount(BigDecimal)}):
 * còn hiệu lực về thời gian, còn lượt, đơn đủ giá trị tối thiểu. Đề bài cấm
 * CHECK / Function / Trigger nên không có ràng buộc nào loại này nằm dưới CSDL.</p>
 *
 * <p>{@code maxDiscount} chỉ có nghĩa với loại {@link DiscountType#PERCENT} - thiếu
 * nó thì mã "giảm 20%" áp vào đơn 100 triệu sẽ giảm mất 20 triệu.</p>
 */
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

    /** Mã khách gõ ở trang thanh toán, ví dụ {@code HELLO2026}. Luôn viết hoa. */
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    /** Không dùng {@code @Enumerated}; xem {@code DiscountTypeConverter}. */
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType = DiscountType.PERCENT;

    /** Số phần trăm (0-100) hoặc số tiền, tuỳ {@link #discountType}. */
    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    /** Trần giảm giá của loại phần trăm; null nghĩa là không chặn trần. */
    @Column(name = "max_discount", precision = 15, scale = 2)
    private BigDecimal maxDiscount;

    /** Giá trị đơn tối thiểu để được áp mã. */
    @Column(name = "min_order_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    /** Tổng số lượt được dùng; null nghĩa là không giới hạn. */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /** Số lượt tối đa cho mỗi tài khoản; null nghĩa là không giới hạn. */
    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser;

    /**
     * Số lượt đã dùng, cộng dồn khi đặt tour thành công.
     *
     * <p>Lưu sẵn thay vì đếm {@code coupon_usages} mỗi lần kiểm mã: khâu kiểm mã
     * nằm ngay trên đường đi của nút "Đặt tour", đếm lại cả bảng ở đó là chỗ chậm
     * không đáng có.</p>
     */
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // ---------------------------------------------------------------------

    /** Còn bật, đang trong khoảng thời gian hiệu lực, và còn lượt. */
    public boolean isRunning(LocalDateTime now) {
        return active
                && startAt != null && !now.isBefore(startAt)
                && endAt != null && !now.isAfter(endAt)
                && hasQuotaLeft();
    }

    public boolean hasQuotaLeft() {
        return usageLimit == null || usedCount == null || usedCount < usageLimit;
    }

    /**
     * Số tiền được giảm cho một đơn có giá trị {@code orderAmount}.
     *
     * <p>Trả về 0 nếu đơn chưa đạt giá trị tối thiểu. Kết quả không bao giờ vượt
     * quá giá trị đơn - không có chuyện áp mã xong thành tiền âm.</p>
     */
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

    /**
     * Trả lại một lượt dùng khi đơn dùng mã này bị huỷ (mục 12.3/12.4) - kẹp sàn
     * ở 0 để không bao giờ âm, phòng khi dữ liệu đã lệch từ trước.
     */
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
