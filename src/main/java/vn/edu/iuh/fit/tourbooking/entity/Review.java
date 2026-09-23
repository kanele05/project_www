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

// Đánh giá của khách về một tour, chờ quản trị viên duyệt mới hiện công khai.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id",
            foreignKey = @ForeignKey(name = "fk_reviews_booking"))
    private Booking booking;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "approved", nullable = false)
    private boolean approved = false;

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

    public boolean isVerified() {
        return booking != null;
    }

    // Quản trị viên trả lời đánh giá, ghi lại thời điểm trả lời.
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
