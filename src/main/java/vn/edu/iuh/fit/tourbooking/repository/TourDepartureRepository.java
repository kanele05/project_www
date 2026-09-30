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

// Truy vấn lịch khởi hành, kể cả tìm lịch gần nhất còn đủ chỗ cho ô chọn ngày AJAX và giỏ hàng.
@Repository
public interface TourDepartureRepository extends JpaRepository<TourDeparture, Long> {

    List<TourDeparture> findByTourIdOrderByDepartureDateAsc(Long tourId);

    boolean existsByTourIdAndDepartureDate(Long tourId, LocalDate departureDate);

    boolean existsByTourIdAndDepartureDateAndIdNot(Long tourId, LocalDate departureDate, Long id);

    // Các lịch khởi hành còn đặt được: tour và lịch đều đang bán, chưa quá hạn chót, còn ít nhất 1 chỗ.
    @Query("""
            SELECT d FROM TourDeparture d
            WHERE d.tour.id = :tourId
              AND d.active = true
              AND d.tour.active = true
              AND d.departureDate >= :minDepartureDate
              AND d.availableSeats > 0
            ORDER BY d.departureDate ASC
            """)
    List<TourDeparture> findBookable(@Param("tourId") Long tourId,
                                     @Param("minDepartureDate") LocalDate minDepartureDate);

    // Lịch khởi hành gần nhất còn ĐỦ chỗ cho minSeats khách (không chỉ còn chỗ, phải đủ) - dùng khi thêm giỏ theo tour.
    default Optional<TourDeparture> findNextBookable(Long tourId, int minSeats, int cutoffDays) {
        return findBookable(tourId, LocalDate.now().plusDays(cutoffDays)).stream()
                .filter(d -> d.hasEnoughSeats(minSeats))
                .findFirst();
    }

    @EntityGraph(attributePaths = {"tour", "tour.category"})
    Optional<TourDeparture> findWithTourById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM TourDeparture d WHERE d.id = :id")
    Optional<TourDeparture> findByIdForUpdate(@Param("id") Long id);
}
