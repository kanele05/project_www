package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.EventType;
import vn.edu.iuh.fit.tourbooking.entity.UserEvent;

import java.time.LocalDateTime;
import java.util.List;

// Truy vấn thống kê hành vi người dùng (JPQL thuần): top tour theo loại sự kiện, phễu, từ khoá, thời gian xem, tín hiệu cho gợi ý.
@Repository
public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    long countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT COUNT(DISTINCT e.sessionId) FROM UserEvent e
            WHERE e.occurredAt >= :from AND e.occurredAt < :to
            """)
    long countDistinctSessions(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT COUNT(DISTINCT e.sessionId) FROM UserEvent e
            WHERE e.type = :type AND e.occurredAt >= :from AND e.occurredAt < :to
            """)
    long countDistinctSessionsByType(@Param("type") EventType type,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    @Query("""
            SELECT t.id, t.name, COUNT(e) FROM UserEvent e
            JOIN Tour t ON t.id = e.tourId
            WHERE e.type = :type AND e.occurredAt >= :from AND e.occurredAt < :to
            GROUP BY t.id, t.name
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> topToursByType(@Param("type") EventType type,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  Pageable pageable);

    @Query("""
            SELECT t.id, t.name,
                   SUM(CASE WHEN e.type = :view THEN 1 ELSE 0 END),
                   SUM(CASE WHEN e.type = :click THEN 1 ELSE 0 END),
                   SUM(CASE WHEN e.type = :cart THEN 1 ELSE 0 END)
            FROM UserEvent e
            JOIN Tour t ON t.id = e.tourId
            WHERE e.occurredAt >= :from AND e.occurredAt < :to
            GROUP BY t.id, t.name
            """)
    List<Object[]> funnelByTour(@Param("view") EventType view,
                                @Param("click") EventType click,
                                @Param("cart") EventType cart,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    @Query("""
            SELECT LOWER(e.keyword), COUNT(e) FROM UserEvent e
            WHERE e.type = :type AND e.keyword IS NOT NULL
              AND e.occurredAt >= :from AND e.occurredAt < :to
            GROUP BY LOWER(e.keyword)
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> topKeywords(@Param("type") EventType type,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               Pageable pageable);

    @Query("""
            SELECT AVG(e.durationMs) FROM UserEvent e
            WHERE e.type = :type AND e.durationMs IS NOT NULL
              AND e.occurredAt >= :from AND e.occurredAt < :to
            """)
    Double averageDuration(@Param("type") EventType type,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to);

    @Query("""
            SELECT t.id, t.name, AVG(e.durationMs), COUNT(e) FROM UserEvent e
            JOIN Tour t ON t.id = e.tourId
            WHERE e.type = :type AND e.durationMs IS NOT NULL
              AND e.occurredAt >= :from AND e.occurredAt < :to
            GROUP BY t.id, t.name
            ORDER BY AVG(e.durationMs) DESC
            """)
    List<Object[]> averageDurationByTour(@Param("type") EventType type,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         Pageable pageable);

    // Tín hiệu gần đây của một tài khoản: (tourId, loại sự kiện), mới nhất trước.
    @Query("""
            SELECT e.tourId, e.type FROM UserEvent e
            WHERE e.userId = :userId AND e.tourId IS NOT NULL AND e.occurredAt >= :since
            ORDER BY e.occurredAt DESC
            """)
    List<Object[]> recentSignalsByUser(@Param("userId") Long userId,
                                       @Param("since") LocalDateTime since,
                                       Pageable pageable);

    // Tín hiệu gần đây của một phiên ẩn danh (sessionId đã băm).
    @Query("""
            SELECT e.tourId, e.type FROM UserEvent e
            WHERE e.sessionId = :sessionId AND e.tourId IS NOT NULL AND e.occurredAt >= :since
            ORDER BY e.occurredAt DESC
            """)
    List<Object[]> recentSignalsBySession(@Param("sessionId") String sessionId,
                                          @Param("since") LocalDateTime since,
                                          Pageable pageable);

    @Modifying
    @Query("DELETE FROM UserEvent e WHERE e.occurredAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
