package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatusHistory;

import java.util.List;

@Repository
// Truy vấn nhật ký đổi trạng thái đơn.
public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {

    List<BookingStatusHistory> findByBookingIdOrderByChangedAtAscIdAsc(Long bookingId);

    @Query("SELECT h FROM BookingStatusHistory h LEFT JOIN FETCH h.changedBy "
            + "WHERE h.booking.code = :code "
            + "ORDER BY h.changedAt ASC, h.id ASC")
    List<BookingStatusHistory> findByBookingCode(@Param("code") String code);

    long countByBookingId(Long bookingId);

    long countByChangedById(Long userId);
}
