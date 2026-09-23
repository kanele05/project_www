package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.TourImage;

import java.util.List;

@Repository
// Truy vấn ảnh của tour.
public interface TourImageRepository extends JpaRepository<TourImage, Long> {

    List<TourImage> findByTourIdOrderBySortOrderAscIdAsc(Long tourId);

    @Query("SELECT i.imagePath FROM TourImage i WHERE i.tour.id = :tourId")
    List<String> findImagePathsByTourId(@Param("tourId") Long tourId);

    long countByTourId(Long tourId);
}
