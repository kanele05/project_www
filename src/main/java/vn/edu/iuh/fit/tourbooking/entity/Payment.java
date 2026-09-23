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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

// Một lần thanh toán của đơn hàng (một đơn có thể có nhiều lần thanh toán: đặt cọc, trả nốt).
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_booking", columnList = "booking_id"),
                @Index(name = "idx_payments_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Payment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_booking"))
    private Booking booking;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "method", nullable = false, length = 30)
    private PaymentMethod method = PaymentMethod.BANK_TRANSFER;

    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "txn_ref", length = 50)
    private String txnRef;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "note", length = 255)
    private String note;

    public Payment(Booking booking, BigDecimal amount, PaymentMethod method) {
        this.booking = booking;
        this.amount = amount;
        this.method = method;
    }

    // Đánh dấu khoản này đã thu tiền, ghi mã giao dịch và thời điểm thanh toán.
    public void markPaid(String txnRef) {
        this.status = PaymentStatus.PAID;
        this.txnRef = txnRef;
        this.paidAt = LocalDateTime.now();
    }

    public boolean isSettled() {
        return status != null && status.isSettled();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
