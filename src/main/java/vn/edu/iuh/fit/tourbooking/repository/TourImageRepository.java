package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.TourImage;

import java.util.List;

/**
 * Truy vấn ảnh của tour.
 *
 * <p>Phần lớn thao tác đi qua {@code Tour.images} (cascade ALL + orphanRemoval);
 * repository này phục vụ các trường hợp cần đọc/xoá ảnh riêng lẻ ở màn quản trị.</p>
 */
@Repository
public interface TourImageRepository extends JpaRepository<TourImage, Long> {

    List<TourImage> findByTourIdOrderBySortOrderAscIdAsc(Long tourId);

    /** Lấy danh sách đường dẫn để xoá file vật lý sau khi giao dịch đã commit. */
    @Query("SELECT i.imagePath FROM TourImage i WHERE i.tour.id = :tourId")
    List<String> findImagePathsByTourId(@Param("tourId") Long tourId);

    long countByTourId(Long tourId);
}
