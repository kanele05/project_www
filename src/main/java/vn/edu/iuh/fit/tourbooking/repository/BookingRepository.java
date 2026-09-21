package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Truy vấn đơn đặt tour.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>,
        JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Lịch sử đặt tour của một khách hàng. Đây là lý do {@code User} không map
     * {@code List<Booking>}: ở đây có phân trang, ở collection thì không.
     */
    @EntityGraph(attributePaths = "user")
    Page<Booking> findByUserIdOrderByBookingDateDesc(Long userId, Pageable pageable);

    /**
     * <b>Truy vấn chặn xoá người dùng.</b> Tài khoản đã từng đặt tour thì không
     * được xoá - đơn hàng là chứng từ, xoá đi sẽ mất dấu vết. Thay vào đó gợi ý
     * quản trị viên vô hiệu hoá tài khoản.
     */
    long countByUserId(Long userId);

    /**
     * Nạp đơn kèm các dòng chi tiết cho trang xem chi tiết đơn.
     *
     * <p>{@code b.promotion} cũng phải nạp sẵn ở đây: trang chi tiết hiện mã giảm
     * giá, mà {@code open-in-view} đang tắt nên chạm vào quan hệ LAZY lúc dựng
     * khuôn mẫu sẽ ném {@code LazyInitializationException}. Triệu chứng rất dễ đi
     * lạc: trang vẫn trả <b>200</b> và hiện được nửa trên, phần HTML sau chỗ lỗi
     * bị cắt cụt giữa chừng vì phần đầu đã kịp gửi đi.</p>
     */
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

    long countByStatus(BookingStatus status);

    /** Tổng doanh thu, chỉ tính các đơn chưa bị huỷ. */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status <> :excluded")
    BigDecimal sumTotalAmountExcludingStatus(@Param("excluded") BookingStatus excluded);

    /**
     * Doanh thu theo tháng cho biểu đồ ở bảng điều khiển quản trị.
     * Trả về mảng {@code [năm, tháng, tổng tiền]}.
     */
    @Query("""
            SELECT YEAR(b.bookingDate), MONTH(b.bookingDate), SUM(b.totalAmount)
            FROM Booking b
            WHERE b.status <> :excluded AND b.bookingDate >= :from
            GROUP BY YEAR(b.bookingDate), MONTH(b.bookingDate)
            ORDER BY YEAR(b.bookingDate), MONTH(b.bookingDate)
            """)
    List<Object[]> revenueByMonth(@Param("from") LocalDateTime from,
                                  @Param("excluded") BookingStatus excluded);

    @EntityGraph(attributePaths = "user")
    List<Booking> findTop5ByOrderByBookingDateDesc();

    /**
     * Tìm kiếm ở màn quản trị: gõ mã đơn, tên hoặc email khách đều ra, lọc thêm
     * được theo trạng thái.
     */
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
