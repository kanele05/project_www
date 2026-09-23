package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.TourItinerary;

import java.util.List;

@Repository
// Truy vấn lịch trình từng ngày của tour.
public interface TourItineraryRepository extends JpaRepository<TourItinerary, Long> {

    List<TourItinerary> findByTourIdOrderByDayNoAscIdAsc(Long tourId);

    long countByTourId(Long tourId);

    boolean existsByTourIdAndDayNo(Long tourId, Integer dayNo);
}
