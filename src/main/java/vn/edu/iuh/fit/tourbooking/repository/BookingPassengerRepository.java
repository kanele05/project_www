package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.BookingPassenger;
import vn.edu.iuh.fit.tourbooking.entity.PassengerType;

import java.util.List;

// Truy vấn danh sách hành khách theo dòng chi tiết hoặc theo cả đơn hàng.
@Repository
public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Long> {

    List<BookingPassenger> findByDetailIdOrderByPassengerTypeAscIdAsc(Long detailId);

    long countByDetailId(Long detailId);

    long countByDetailIdAndPassengerType(Long detailId, PassengerType passengerType);

    @Query("SELECT p FROM BookingPassenger p JOIN FETCH p.detail det "
            + "WHERE det.booking.id = :bookingId "
            + "ORDER BY det.id ASC, p.passengerType ASC, p.id ASC")
    List<BookingPassenger> findByBookingId(@Param("bookingId") Long bookingId);
}
