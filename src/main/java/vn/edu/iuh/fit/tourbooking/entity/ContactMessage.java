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
 * Một liên hệ gửi từ biểu mẫu công khai.
 *
 * <p>Người gửi <b>không bắt buộc có tài khoản</b>, nên bảng lưu thẳng họ tên /
 * email / điện thoại thay vì trỏ sang {@link User}. Nếu khách đang đăng nhập thì
 * biểu mẫu chỉ điền sẵn giúp, dữ liệu vẫn được chép vào đây - liên hệ là chứng từ
 * tại thời điểm gửi, giống nguyên tắc ở {@link Booking}.</p>
 *
 * <p>{@code tour} để null được: khách có thể hỏi chung, hoặc bấm "Tư vấn tour này"
 * từ trang chi tiết thì mới có tour cụ thể.</p>
 */
@Entity
@Table(
        name = "contact_messages",
        indexes = {
                @Index(name = "idx_contact_messages_status", columnList = "status"),
                @Index(name = "idx_contact_messages_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ContactMessage extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    /** Không dùng {@code @Enumerated}; xem {@code ContactStatusConverter}. */
    @Column(name = "status", nullable = false, length = 20)
    private ContactStatus status = ContactStatus.NEW;

    /** Tour mà khách đang hỏi, nếu liên hệ gửi đi từ trang chi tiết tour. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id",
            foreignKey = @ForeignKey(name = "fk_contact_messages_tour"))
    private Tour tour;

    /** Nhân viên đã tiếp nhận. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_id",
            foreignKey = @ForeignKey(name = "fk_contact_messages_user"))
    private User handledBy;

    /** Nội dung đã trả lời khách, lưu để người sau đọc lại được. */
    @Column(name = "reply_note", length = 1000)
    private String replyNote;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    public ContactMessage(String fullName, String email, String subject, String content) {
        this.fullName = fullName;
        this.email = email;
        this.subject = subject;
        this.content = content;
    }

    /** Đánh dấu đã xử lý: đổi trạng thái, ghi người xử lý và mốc thời gian cùng lúc. */
    public void resolve(User staff, String replyNote) {
        this.status = ContactStatus.RESOLVED;
        this.handledBy = staff;
        this.replyNote = replyNote;
        this.handledAt = LocalDateTime.now();
    }

    public boolean isNew() {
        return status == ContactStatus.NEW;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContactMessage other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
