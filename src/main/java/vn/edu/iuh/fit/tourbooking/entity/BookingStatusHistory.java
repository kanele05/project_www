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

import java.time.LocalDateTime;

/**
 * Nhật ký đổi trạng thái đơn: đơn nào, từ trạng thái nào sang trạng thái nào,
 * ai đổi, lúc mấy giờ, vì lý do gì.
 *
 * <p>Cột {@code bookings.status} chỉ giữ trạng thái <b>hiện tại</b>; bảng này giữ
 * cả quá trình. Khi khách gọi hỏi "sao đơn tôi bị huỷ", đây là chỗ trả lời được.
 * Bản ghi chỉ thêm chứ không sửa - ứng dụng không có chỗ nào cập nhật lại dòng
 * lịch sử.</p>
 *
 * <p>{@code changedBy} để null được: hệ thống tự đổi trạng thái (ví dụ tour đã
 * kết thúc thì chuyển sang COMPLETED) thì không có người nào đứng sau.</p>
 */
@Entity
@Table(
        name = "booking_status_history",
        indexes = {
                @Index(name = "idx_booking_status_history_booking", columnList = "booking_id"),
                @Index(name = "idx_booking_status_history_time", columnList = "changed_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_booking_status_history_booking"))
    private Booking booking;

    /** Null ở dòng đầu tiên - lúc đơn vừa được tạo thì chưa có trạng thái cũ. */
    @Column(name = "from_status", length = 20)
    private BookingStatus fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private BookingStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id",
            foreignKey = @ForeignKey(name = "fk_booking_status_history_user"))
    private User changedBy;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    public BookingStatusHistory(Booking booking, BookingStatus fromStatus,
                                BookingStatus toStatus, User changedBy, String reason) {
        this.booking = booking;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.reason = reason;
        this.changedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookingStatusHistory other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
