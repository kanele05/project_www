package vn.edu.iuh.fit.tourbooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Một sự kiện hành vi trên giao diện (xem, bấm, thêm giỏ, tìm kiếm, thời gian xem); không có khoá ngoại để ghi nhanh và không cản việc xoá tour.
@Entity
@Table(
        name = "user_events",
        indexes = {
                @Index(name = "idx_user_events_tour", columnList = "tour_id, event_type"),
                @Index(name = "idx_user_events_time", columnList = "occurred_at"),
                @Index(name = "idx_user_events_session", columnList = "session_id"),
                @Index(name = "idx_user_events_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 20)
    private EventType type;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "tour_id")
    private Long tourId;

    @Column(name = "departure_id")
    private Long departureId;

    @Column(name = "keyword", length = 200)
    private String keyword;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "page_url", length = 255)
    private String pageUrl;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();
}
