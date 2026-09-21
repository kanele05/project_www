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

/**
 * Đánh giá của khách về một tour đã đi.
 *
 * <p>Hai quy tắc làm nên giá trị của bảng này, cả hai đều kiểm bằng Java ở tầng
 * service:</p>
 * <ol>
 *   <li><b>Chỉ khách đã thực sự đi</b> mới được đánh giá - phải có một đơn ở trạng
 *       thái {@code COMPLETED} chứa tour này. Cột {@code booking_id} giữ lại đơn đã
 *       dùng làm bằng chứng, nhờ đó hiện được nhãn "Đã xác thực".</li>
 *   <li><b>Mỗi tài khoản đánh giá một tour một lần</b> - ràng buộc duy nhất
 *       {@code (user_id, tour_id)}.</li>
 * </ol>
 *
 * <p>{@code approved} mặc định là {@code false}: đánh giá phải qua kiểm duyệt mới
 * hiện ra trang công khai, tránh nội dung rác. Điểm trung bình của tour chỉ tính
 * trên các đánh giá đã duyệt.</p>
 */
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reviews_user_tour", columnNames = {"user_id", "tour_id"}),
        indexes = {
                @Index(name = "idx_reviews_tour", columnList = "tour_id"),
                @Index(name = "idx_reviews_approved", columnList = "approved")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Review extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_tour"))
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_user"))
    private User user;

    /** Đơn dùng làm bằng chứng đã đi tour; null với dữ liệu nhập tay của quản trị viên. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id",
            foreignKey = @ForeignKey(name = "fk_reviews_booking"))
    private Booking booking;

    /** Số sao từ 1 đến 5. Khoảng giá trị kiểm ở DTO và service, không đặt CHECK. */
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    /** Chỉ đánh giá đã duyệt mới hiện ra trang công khai và mới được tính điểm. */
    @Column(name = "approved", nullable = false)
    private boolean approved = false;

    /** Trả lời của quản trị viên, hiện ngay dưới đánh giá. */
    @Column(name = "admin_reply", length = 1000)
    private String adminReply;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    public Review(Tour tour, User user, Integer rating, String content) {
        this.tour = tour;
        this.user = user;
        this.rating = rating;
        this.content = content;
    }

    /** Đánh giá gắn với một đơn đã hoàn thành thì được gắn nhãn "Đã xác thực". */
    public boolean isVerified() {
        return booking != null;
    }

    public void reply(String text) {
        this.adminReply = text;
        this.repliedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Review other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
