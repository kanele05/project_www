package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.BookingDetail;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;

import java.util.Collection;
import java.util.List;

// Truy vấn dòng chi tiết đơn: đếm phục vụ quy tắc chặn xoá, top tour bán chạy, điều kiện đánh giá.
@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {

    long countByDepartureId(Long departureId);

    long countByDeparture_Tour_Id(Long tourId);

    @Query("SELECT COUNT(DISTINCT d.booking.id) FROM BookingDetail d WHERE d.departure.tour.id = :tourId")
    long countDistinctBookingsByTourId(@Param("tourId") Long tourId);

    List<BookingDetail> findByBookingId(Long bookingId);

    @Query("""
            SELECT d.tourNameSnapshot, SUM(d.numAdults + d.numChildren)
            FROM BookingDetail d
            WHERE d.booking.status IN :statuses
            GROUP BY d.tourNameSnapshot
            ORDER BY SUM(d.numAdults + d.numChildren) DESC
            """)
    List<Object[]> findTopSellingTours(@Param("statuses") Collection<BookingStatus> statuses,
                                       org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT COUNT(d) > 0 FROM BookingDetail d
            WHERE d.departure.tour.id = :tourId
              AND d.booking.user.id = :userId
              AND d.booking.status = vn.edu.iuh.fit.tourbooking.entity.BookingStatus.COMPLETED
            """)
    boolean existsCompletedBookingForTour(@Param("tourId") Long tourId, @Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT d.booking FROM BookingDetail d
            WHERE d.departure.tour.id = :tourId
              AND d.booking.user.id = :userId
              AND d.booking.status = vn.edu.iuh.fit.tourbooking.entity.BookingStatus.COMPLETED
            ORDER BY d.booking.bookingDate DESC
            """)
    List<Booking> findCompletedBookingsForTour(@Param("tourId") Long tourId, @Param("userId") Long userId);
}
