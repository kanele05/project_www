package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.CouponUsage;

import java.util.List;
import java.util.Optional;

@Repository
// Truy vấn lượt dùng mã khuyến mãi.
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    long countByPromotionIdAndUserId(Long promotionId, Long userId);

    long countByPromotionId(Long promotionId);

    long countByUserId(Long userId);

    Optional<CouponUsage> findByBookingId(Long bookingId);

    List<CouponUsage> findByUserIdOrderByUsedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "booking"})
    Page<CouponUsage> findByPromotionIdOrderByUsedAtDesc(Long promotionId, Pageable pageable);
}
