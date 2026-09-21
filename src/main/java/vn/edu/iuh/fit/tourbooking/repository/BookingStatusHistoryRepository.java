package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatusHistory;

import java.util.List;

/** Truy vấn nhật ký đổi trạng thái đơn. Bảng chỉ ghi thêm, không sửa, không xoá. */
@Repository
public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {

    List<BookingStatusHistory> findByBookingIdOrderByChangedAtAscIdAsc(Long bookingId);

    /**
     * Dòng thời gian tra theo mã đơn - trang chi tiết đơn dùng mã chứ không dùng id.
     *
     * <p>{@code LEFT JOIN FETCH h.changedBy} là bắt buộc: cả hai màn chi tiết đơn
     * (khách hàng lẫn quản trị viên) hiện tên người đổi trạng thái ngay trên dòng
     * thời gian, mà {@code open-in-view} đang tắt nên chạm vào quan hệ LAZY
     * {@code changedBy} chưa nạp sẽ cắt cụt trang giữa chừng dù vẫn trả mã 200
     * (gotcha #43 - vấp thật lần này: {@code LazyInitializationException} khi mở
     * trang bằng một quản trị viên KHÁC người vừa đổi trạng thái đơn).</p>
     */
    @Query("SELECT h FROM BookingStatusHistory h LEFT JOIN FETCH h.changedBy "
            + "WHERE h.booking.code = :code "
            + "ORDER BY h.changedAt ASC, h.id ASC")
    List<BookingStatusHistory> findByBookingCode(@Param("code") String code);

    long countByBookingId(Long bookingId);

    /** Phục vụ quy tắc chặn xoá tài khoản đã từng thao tác trên đơn. */
    long countByChangedById(Long userId);
}
