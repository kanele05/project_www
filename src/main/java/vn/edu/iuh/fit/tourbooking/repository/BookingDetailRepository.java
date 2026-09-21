package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.BookingDetail;

import java.util.List;

/**
 * Truy vấn chi tiết đơn.
 *
 * <p>Repository này tồn tại chủ yếu vì hai truy vấn đếm bên dưới: chúng là chỗ
 * hiện thực yêu cầu "không cho xoá dữ liệu đang được tham chiếu" của đề bài,
 * kiểm tra bằng Java ở tầng service chứ không nhờ CSDL.</p>
 */
@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {

    /** <b>Chặn xoá đợt khởi hành</b> đã có khách đặt. */
    long countByDepartureId(Long departureId);

    /** <b>Chặn xoá tour</b> khi bất kỳ đợt khởi hành nào của nó đã có khách đặt. */
    long countByDeparture_Tour_Id(Long tourId);

    List<BookingDetail> findByBookingId(Long bookingId);

    /** Xếp hạng tour bán chạy cho bảng điều khiển: {@code [tên tour, số khách]}. */
    @Query("""
            SELECT d.tourNameSnapshot, SUM(d.numAdults + d.numChildren)
            FROM BookingDetail d
            GROUP BY d.tourNameSnapshot
            ORDER BY SUM(d.numAdults + d.numChildren) DESC
            """)
    List<Object[]> findTopSellingTours(org.springframework.data.domain.Pageable pageable);

    /**
     * <b>Điều kiện được phép đánh giá (UC018):</b> tài khoản phải có ít nhất một
     * đơn ở trạng thái {@code COMPLETED} chứa tour này.
     */
    @Query("""
            SELECT COUNT(d) > 0 FROM BookingDetail d
            WHERE d.departure.tour.id = :tourId
              AND d.booking.user.id = :userId
              AND d.booking.status = vn.edu.iuh.fit.tourbooking.entity.BookingStatus.COMPLETED
            """)
    boolean existsCompletedBookingForTour(@Param("tourId") Long tourId, @Param("userId") Long userId);

    /**
     * Đơn đã hoàn tất dùng làm "bằng chứng đã đi" khi ghi nhận đánh giá - xem
     * {@code Review.booking}. Mới nhất trước, phòng khi khách đi tour này nhiều lần.
     */
    @Query("""
            SELECT DISTINCT d.booking FROM BookingDetail d
            WHERE d.departure.tour.id = :tourId
              AND d.booking.user.id = :userId
              AND d.booking.status = vn.edu.iuh.fit.tourbooking.entity.BookingStatus.COMPLETED
            ORDER BY d.booking.bookingDate DESC
            """)
    List<Booking> findCompletedBookingsForTour(@Param("tourId") Long tourId, @Param("userId") Long userId);
}
