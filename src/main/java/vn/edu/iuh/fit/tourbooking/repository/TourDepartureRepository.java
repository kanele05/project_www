package vn.edu.iuh.fit.tourbooking.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Truy vấn đợt khởi hành.
 */
@Repository
public interface TourDepartureRepository extends JpaRepository<TourDeparture, Long> {

    List<TourDeparture> findByTourIdOrderByDepartureDateAsc(Long tourId);

    boolean existsByTourIdAndDepartureDate(Long tourId, LocalDate departureDate);

    boolean existsByTourIdAndDepartureDateAndIdNot(Long tourId, LocalDate departureDate, Long id);

    /**
     * Các đợt khách còn đặt được, dùng cho ô chọn ngày ở trang chi tiết và cho
     * web service {@code /api/tours/{id}/departures}.
     */
    @Query("""
            SELECT d FROM TourDeparture d
            WHERE d.tour.id = :tourId
              AND d.active = true
              AND d.tour.active = true
              AND d.departureDate > :today
              AND d.availableSeats > 0
            ORDER BY d.departureDate ASC
            """)
    List<TourDeparture> findBookable(@Param("tourId") Long tourId, @Param("today") LocalDate today);

    /**
     * Đợt gần nhất còn <b>đủ</b> chỗ cho {@code minSeats} khách. Nút "Đặt tour" ở
     * trang danh sách không cho chọn ngày, nên giỏ hàng tự chọn giúp khách đợt sớm
     * nhất; ở trang chi tiết khách vẫn đổi được sang đợt khác.
     *
     * <p><b>Lọc theo {@code minSeats} chứ không chỉ lấy đợt gần nhất rồi để
     * {@code CartService} tự báo lỗi.</b> Trước bản vá này, khách xin 10 chỗ mà
     * đợt gần nhất chỉ còn 3 sẽ nhận lỗi "không đủ chỗ" dù đợt kế tiếp còn thừa
     * chỗ - đúng ra phải tự động bỏ qua đợt không đủ và chọn đợt gần nhất mà thực
     * sự đặt được.</p>
     */
    default Optional<TourDeparture> findNextBookable(Long tourId, int minSeats) {
        return findBookable(tourId, LocalDate.now()).stream()
                .filter(d -> d.hasEnoughSeats(minSeats))
                .findFirst();
    }

    /**
     * Nạp kèm tour và danh mục để dùng trong giỏ hàng / thanh toán, nơi cần tên
     * tour và ảnh mà {@code open-in-view} đang tắt.
     */
    @EntityGraph(attributePaths = {"tour", "tour.category"})
    Optional<TourDeparture> findWithTourById(Long id);

    /**
     * Khoá bi quan khi đặt tour: hai khách cùng lấy chỗ cuối cùng thì giao dịch
     * thứ hai phải chờ, đọc lại {@code availableSeats} đã cập nhật rồi mới quyết
     * định - đây là chốt chặn cuối cùng bổ sung cho {@code @Version}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM TourDeparture d WHERE d.id = :id")
    Optional<TourDeparture> findByIdForUpdate(@Param("id") Long id);
}
