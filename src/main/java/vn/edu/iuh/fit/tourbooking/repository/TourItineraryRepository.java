package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.TourItinerary;

import java.util.List;

/**
 * Truy vấn lịch trình từng ngày của tour.
 *
 * <p>Phần lớn thao tác đi qua {@code Tour.itineraries} (cascade ALL +
 * orphanRemoval); repository này phục vụ màn quản trị khi cần đọc riêng.</p>
 */
@Repository
public interface TourItineraryRepository extends JpaRepository<TourItinerary, Long> {

    List<TourItinerary> findByTourIdOrderByDayNoAscIdAsc(Long tourId);

    long countByTourId(Long tourId);

    /** Chặn nhập trùng "ngày 2" cho cùng một tour, báo lỗi trước khi đụng CSDL. */
    boolean existsByTourIdAndDayNo(Long tourId, Integer dayNo);
}
