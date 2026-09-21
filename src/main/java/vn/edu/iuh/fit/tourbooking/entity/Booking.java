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

/**
 * Đơn đặt tour - gốc của cụm dữ liệu {@code Booking + BookingDetail}.
 *
 * <p><b>Vì sao chép lại thông tin liên hệ của khách vào đơn</b> thay vì luôn đọc
 * từ {@link User}: đơn hàng là chứng từ, phải phản ánh đúng thông tin tại thời
 * điểm đặt. Khách đổi số điện thoại sáu tháng sau thì đơn cũ vẫn phải giữ số cũ.
 * Cùng lý do đó, {@link BookingDetail} chép lại tên tour và đơn giá.</p>
 *
 * <p>URL tra cứu đơn dùng {@code code} chứ không dùng {@code id} tuần tự, kèm
 * kiểm tra quyền sở hữu ở controller - sửa số trên thanh địa chỉ không xem được
 * đơn của người khác.</p>
 */
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

    /** Mã đơn hiển thị cho khách, dạng {@code TB20260807A1B2C3}. */
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bookings_user"))
    private User user;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    // --- Thông tin liên hệ tại thời điểm đặt (bản sao, không đọc từ User) ---

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 150)
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "customer_address", length = 255)
    private String customerAddress;

    /**
     * Số tiền khách phải trả, <b>đã trừ</b> {@link #discountAmount}.
     * Luôn được tính lại bằng {@link #recalculateTotal()}, không gán tay.
     */
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Mã giảm giá đã áp cho đơn, null nếu khách không dùng mã.
     *
     * <p>Bảng {@code coupon_usages} có ràng buộc duy nhất trên {@code booking_id}
     * nên quan hệ này luôn là một - một về phía đơn: mỗi đơn tối đa một mã.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id",
            foreignKey = @ForeignKey(name = "fk_bookings_promotion"))
    private Promotion promotion;

    /**
     * Số tiền được giảm, <b>chép lại</b> tại thời điểm đặt.
     *
     * <p>Không tính lại từ {@link Promotion} mỗi lần đọc: quản trị viên sửa giá trị
     * mã về sau thì hoá đơn cũ vẫn phải giữ nguyên số đã giảm - cùng nguyên tắc với
     * các cột bản sao ở {@link BookingDetail}.</p>
     */
    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Không dùng {@code @Enumerated}; việc quy đổi do {@code BookingStatusConverter}
     * đảm nhiệm để Hibernate không sinh ràng buộc CHECK cho cột enum
     * (xem chú thích ở {@code RoleConverter} và ở {@code User.role} về lý do
     * không ép kiểu cột thành VARCHAR).
     */
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    /**
     * Chi tiết đơn không tồn tại độc lập, nên {@code cascade = ALL} +
     * {@code orphanRemoval}: xoá dòng khỏi danh sách là xoá luôn bản ghi.
     */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingDetail> details = new ArrayList<>();

    // ---------------------------------------------------------------------

    public void addDetail(BookingDetail detail) {
        details.add(detail);
        detail.setBooking(this);
    }

    public void removeDetail(BookingDetail detail) {
        details.remove(detail);
        detail.setBooking(null);
    }

    /** Tiền hàng trước khi trừ giảm giá. */
    public BigDecimal getSubtotalAmount() {
        return details.stream()
                .map(BookingDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Tính lại tổng tiền - gọi sau mọi thay đổi số lượng khách hoặc mã giảm giá.
     *
     * <p>Có kẹp sàn ở 0: giảm giá không bao giờ được lớn hơn tiền hàng, kể cả khi
     * quản trị viên lỡ tay nhập một mã giảm 50 triệu.</p>
     */
    public void recalculateTotal() {
        BigDecimal subtotal = getSubtotalAmount();
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
            this.discountAmount = discount;
        }
        this.totalAmount = subtotal.subtract(discount);
    }

    /** Tổng số khách của cả đơn, dùng để hiển thị và để kiểm tra chỗ. */
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
