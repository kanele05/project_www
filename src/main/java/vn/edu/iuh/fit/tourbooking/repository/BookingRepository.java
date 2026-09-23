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

    /**
     * Cùng {@link #findDetailByCode} nhưng tra theo khoá chính - dùng ở
     * {@code BookingService.expirePendingBooking} (mục 12.4): bộ hẹn giờ chỉ có
     * sẵn {@code id} từ {@link #findExpiredPendingIds}, không có mã đơn.
     */
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

    /**
     * <b>Khoá bi quan trên chính đơn</b> (mục "NGHIÊM TRỌNG - 1", đã tái hiện bằng
     * thao tác thật hai kịch bản mất khoản đã thu và lách máy trạng thái).
     * {@code updateStatus}, {@code cancelBySelf}, {@code expirePendingBooking},
     * {@code updateDetailQuantity} và {@code PaymentService.markPaid} (khoá đơn
     * cha trước khi đụng khoản thu) đều phải gọi truy vấn này <b>ĐẦU TIÊN</b>,
     * trước khi đọc {@code status} hay đụng vào {@code payments} - bên thua phải
     * đợi bên thắng commit xong rồi mới đọc lại đúng trạng thái mới nhất.
     *
     * <p>Truy vấn TỐI GIẢN (không JOIN FETCH quan hệ nào): khoá xong còn phải gọi
     * {@code entityManager.refresh(...)} ngay (gotcha #60 - entity có thể đã nằm
     * sẵn trong cache cấp một của cùng giao dịch), JOIN FETCH ở bước khoá chỉ làm
     * phức tạp thêm mà không cần thiết vì các phương thức gọi nó đều đang chạy
     * trong chính giao dịch @Transactional của mình nên đụng quan hệ LAZY sau đó
     * vẫn an toàn.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.code = :code")
    java.util.Optional<Booking> findByCodeForUpdate(@Param("code") String code);

    /** Cùng {@link #findByCodeForUpdate} nhưng theo khoá chính - dùng ở {@code expirePendingBooking}. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    java.util.Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(BookingStatus status);

    /**
     * Mục 12.4: các đơn CHỜ XÁC NHẬN, đặt trước {@code before} và <b>chưa từng</b>
     * có khoản ĐÃ THANH TOÁN - ứng viên để {@code BookingExpiryScheduler} tự huỷ.
     * Chỉ trả về id: mỗi đơn được nạp lại đầy đủ và xử lý trong giao dịch riêng
     * của chính nó ở {@code BookingService.expirePendingBooking}.
     */
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

    /**
     * Tổng doanh thu - chỉ tính đơn ĐÃ XÁC NHẬN + HOÀN TẤT (mục 12.9: loại CHỜ
     * và ĐÃ HUỶ, vì tiền đó chưa chắc chắn thu được).
     */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status IN :statuses")
    BigDecimal sumTotalAmountForStatuses(@Param("statuses") Collection<BookingStatus> statuses);

    /**
     * Doanh thu theo tháng cho biểu đồ ở bảng điều khiển quản trị (mục 12.9: cùng
     * quy tắc đơn tính vào doanh thu như trên).
     * Trả về mảng {@code [năm, tháng, tổng tiền]}.
     */
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
