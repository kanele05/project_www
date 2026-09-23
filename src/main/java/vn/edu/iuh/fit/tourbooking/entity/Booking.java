package vn.edu.iuh.fit.tourbooking.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Đơn đặt tour - chép lại thông tin liên hệ và giá tại thời điểm đặt, tra cứu bằng "code" chứ không dùng id.
@Entity
@Table(
        name = "bookings",
        uniqueConstraints = @UniqueConstraint(name = "uk_bookings_code", columnNames = "code"),
        indexes = {
                @Index(name = "idx_bookings_user", columnList = "user_id"),
                @Index(name = "idx_bookings_status", columnList = "status"),
                @Index(name = "idx_bookings_date", columnList = "booking_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bookings_user"))
    private User user;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 150)
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "customer_address", length = 255)
    private String customerAddress;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id",
            foreignKey = @ForeignKey(name = "fk_bookings_promotion"))
    private Promotion promotion;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingDetail> details = new ArrayList<>();

    // Thêm dòng chi tiết và gắn ngược lại tham chiếu booking (giữ hai chiều nhất quán).
    public void addDetail(BookingDetail detail) {
        details.add(detail);
        detail.setBooking(this);
    }

    // Gỡ một dòng chi tiết khỏi đơn.
    public void removeDetail(BookingDetail detail) {
        details.remove(detail);
        detail.setBooking(null);
    }

    // Cộng dồn thành tiền của tất cả các dòng chi tiết.
    public BigDecimal getSubtotalAmount() {
        return details.stream()
                .map(BookingDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Tính lại tổng tiền = tổng các dòng trừ giảm giá, chặn giảm giá vượt quá tổng.
    public void recalculateTotal() {
        BigDecimal subtotal = getSubtotalAmount();
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
            this.discountAmount = discount;
        }
        this.totalAmount = subtotal.subtract(discount);
    }

    public int getTotalGuests() {
        return details.stream().mapToInt(BookingDetail::getTotalGuests).sum();
    }

    public boolean isCancellable() {
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking other)) return false;
        return code != null && code.equals(other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
