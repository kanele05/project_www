package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.BookingPassenger;
import vn.edu.iuh.fit.tourbooking.entity.PassengerType;

import java.util.List;

/** Truy vấn danh sách hành khách trong đơn. */
@Repository
public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Long> {

    List<BookingPassenger> findByDetailIdOrderByPassengerTypeAscIdAsc(Long detailId);

    long countByDetailId(Long detailId);

    /** Đếm theo loại - dùng để đối chiếu với numAdults / numChildren của dòng. */
    long countByDetailIdAndPassengerType(Long detailId, PassengerType passengerType);

    /**
     * Cả đoàn của một đơn, kể cả khi đơn đặt nhiều tour cùng lúc.
     *
     * <p>{@code JOIN FETCH p.detail} là bắt buộc: màn chi tiết đơn (khách hàng lẫn
     * quản trị viên) nhóm hành khách theo {@code detail.id} trong khuôn mẫu, mà
     * {@code open-in-view} đang tắt nên chạm vào quan hệ LAZY chưa nạp sẽ cắt cụt
     * trang giữa chừng dù vẫn trả mã 200 (gotcha #43).</p>
     */
    @Query("SELECT p FROM BookingPassenger p JOIN FETCH p.detail det "
            + "WHERE det.booking.id = :bookingId "
            + "ORDER BY det.id ASC, p.passengerType ASC, p.id ASC")
    List<BookingPassenger> findByBookingId(@Param("bookingId") Long bookingId);
}
