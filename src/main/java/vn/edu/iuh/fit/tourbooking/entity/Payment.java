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

/**
 * Một lần thanh toán của đơn hàng.
 *
 * <p>Quan hệ với {@link Booking} là <b>một - nhiều</b> chứ không phải một - một,
 * vì thực tế bán tour hay chia làm hai lần: đặt cọc lúc giữ chỗ, trả nốt trước
 * ngày khởi hành. Tổng các lần {@code PAID} mới là số tiền đã thu; so với
 * {@code booking.totalAmount} sẽ ra số còn nợ.</p>
 *
 * <p>{@code txnRef} là mã giao dịch phía ngân hàng / ví điện tử, dùng để đối soát
 * và để một thông báo thanh toán không bị ghi nhận hai lần. Tính duy nhất của nó
 * <b>kiểm bằng Java</b> ({@code PaymentRepository.findByTxnRef}) chứ không khai
 * báo {@code @UniqueConstraint}: SQL Server chỉ cho phép <i>một</i> dòng NULL
 * trong một ràng buộc UNIQUE, mà lần thanh toán nào chưa hoàn tất cũng để trống
 * cột này. Lý do không dùng chỉ mục có lọc ghi ở {@code database/02_schema.sql}.</p>
 */
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

    /**
     * Không dùng {@code @Enumerated}; xem {@code PaymentMethodConverter}.
     */
    @Column(name = "method", nullable = false, length = 30)
    private PaymentMethod method = PaymentMethod.BANK_TRANSFER;

    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    /** Mã giao dịch của ngân hàng hoặc ví điện tử, dùng để đối soát. */
    @Column(name = "txn_ref", length = 50)
    private String txnRef;

    /** Thời điểm thực nhận tiền; còn null khi lần thanh toán chưa hoàn tất. */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "note", length = 255)
    private String note;

    public Payment(Booking booking, BigDecimal amount, PaymentMethod method) {
        this.booking = booking;
        this.amount = amount;
        this.method = method;
    }

    /** Ghi nhận đã thu tiền: đổi trạng thái và đóng mốc thời gian cùng lúc. */
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
