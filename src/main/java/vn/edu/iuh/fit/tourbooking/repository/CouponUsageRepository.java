package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.CouponUsage;

import java.util.List;
import java.util.Optional;

/** Truy vấn lịch sử dùng mã giảm giá. */
@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    /** Chặn quy tắc "mỗi tài khoản chỉ dùng mã này N lượt" trước khi tạo đơn. */
    long countByPromotionIdAndUserId(Long promotionId, Long userId);

    /** Đối chiếu với {@code Promotion.usedCount} khi cần kiểm tra lại số liệu. */
    long countByPromotionId(Long promotionId);

    long countByUserId(Long userId);

    /** Mỗi đơn chỉ áp một mã nên trả về Optional chứ không phải danh sách. */
    Optional<CouponUsage> findByBookingId(Long bookingId);

    List<CouponUsage> findByUserIdOrderByUsedAtDesc(Long userId);

    /**
     * Lượt dùng của một mã cho màn quản trị (UC019: "xem coupon_usages của từng
     * mã"). Nạp kèm {@code user} và {@code booking} - open-in-view đang tắt nên
     * khuôn mẫu chạm vào hai quan hệ LAZY này mà chưa nạp sẽ cắt cụt trang giữa
     * chừng dù vẫn trả mã 200 (gotcha #43).
     */
    @EntityGraph(attributePaths = {"user", "booking"})
    Page<CouponUsage> findByPromotionIdOrderByUsedAtDesc(Long promotionId, Pageable pageable);
}
