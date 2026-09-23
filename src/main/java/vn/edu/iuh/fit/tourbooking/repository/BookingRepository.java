package vn.edu.iuh.fit.tourbooking.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Truy vấn đơn hàng: tra cứu có JOIN FETCH đủ quan hệ, khoá bi quan để cập nhật, và số liệu thống kê.
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>,
        JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "user")
    Page<Booking> findByUserIdOrderByBookingDateDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.user
            LEFT JOIN FETCH b.promotion
            LEFT JOIN FETCH b.details det
            LEFT JOIN FETCH det.departure dep
            LEFT JOIN FETCH dep.tour
            WHERE b.code = :code
            """)
    Optional<Booking> findDetailByCode(@Param("code") String code);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.user
            LEFT JOIN FETCH b.promotion
            LEFT JOIN FETCH b.details det
            LEFT JOIN FETCH det.departure dep
            LEFT JOIN FETCH dep.tour
            WHERE b.id = :id
            """)
    Optional<Booking> findDetailById(@Param("id") Long id);

    // Khoá dòng đơn (SELECT ... FOR UPDATE) trước khi đổi trạng thái, để hai request đồng thời không lost update.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.code = :code")
    java.util.Optional<Booking> findByCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    java.util.Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(BookingStatus status);

    // Id các đơn PENDING đã quá hạn thanh toán và chưa có khoản PAID nào - đầu vào của bộ hẹn giờ tự huỷ.
    @Query("""
            SELECT b.id FROM Booking b
            WHERE b.status = vn.edu.iuh.fit.tourbooking.entity.BookingStatus.PENDING
              AND b.bookingDate < :before
              AND NOT EXISTS (
                  SELECT 1 FROM Payment p
                  WHERE p.booking = b AND p.status = vn.edu.iuh.fit.tourbooking.entity.PaymentStatus.PAID
              )
            """)
    List<Long> findExpiredPendingIds(@Param("before") LocalDateTime before);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status IN :statuses")
    BigDecimal sumTotalAmountForStatuses(@Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            SELECT YEAR(b.bookingDate), MONTH(b.bookingDate), SUM(b.totalAmount)
            FROM Booking b
            WHERE b.status IN :statuses AND b.bookingDate >= :from
            GROUP BY YEAR(b.bookingDate), MONTH(b.bookingDate)
            ORDER BY YEAR(b.bookingDate), MONTH(b.bookingDate)
            """)
    List<Object[]> revenueByMonth(@Param("from") LocalDateTime from,
                                  @Param("statuses") Collection<BookingStatus> statuses);

    @EntityGraph(attributePaths = "user")
    List<Booking> findTop5ByOrderByBookingDateDesc();

    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT b FROM Booking b
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR b.code           LIKE %:keyword%
                   OR b.customerName   LIKE %:keyword%
                   OR b.customerEmail  LIKE %:keyword%
                   OR b.customerPhone  LIKE %:keyword%)
              AND (:status IS NULL OR b.status = :status)
            """)
    Page<Booking> adminSearch(@Param("keyword") String keyword,
                              @Param("status") BookingStatus status,
                              Pageable pageable);
}
