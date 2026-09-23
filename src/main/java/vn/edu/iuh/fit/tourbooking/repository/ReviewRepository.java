package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.Review;

import java.util.Optional;

@Repository
// Truy vấn đánh giá tour, lọc theo trạng thái duyệt.
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "user")
    Page<Review> findByTourIdAndApprovedTrueOrderByCreatedAtDesc(Long tourId, Pageable pageable);

    @EntityGraph(attributePaths = {"tour", "user"})
    Page<Review> findByApprovedFalseOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"tour", "user"})
    Page<Review> findByApprovedTrueOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"tour", "user"})
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Review> findByUserIdAndTourId(Long userId, Long tourId);

    boolean existsByUserIdAndTourId(Long userId, Long tourId);

    long countByTourIdAndApprovedTrue(Long tourId);

    long countByApprovedFalse();

    long countByTourId(Long tourId);

    long countByUserId(Long userId);

    @Query("SELECT AVG(CAST(r.rating AS double)) FROM Review r "
            + "WHERE r.tour.id = :tourId AND r.approved = true")
    Double averageRatingByTourId(@Param("tourId") Long tourId);
}
