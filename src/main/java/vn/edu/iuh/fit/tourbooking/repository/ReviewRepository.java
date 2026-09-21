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

/** Truy vấn đánh giá tour. */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Danh sách hiện cho khách xem: chỉ những đánh giá đã duyệt.
     *
     * <p>{@code @EntityGraph} nạp sẵn {@code user} - trang chi tiết tour hiện tên
     * người đánh giá, mà {@code open-in-view} đang tắt nên chạm vào quan hệ LAZY
     * lúc dựng khuôn mẫu sẽ cắt cụt trang giữa chừng (gotcha #43).</p>
     */
    @EntityGraph(attributePaths = "user")
    Page<Review> findByTourIdAndApprovedTrueOrderByCreatedAtDesc(Long tourId, Pageable pageable);

    /**
     * Ba truy vấn bên dưới phục vụ màn kiểm duyệt {@code /admin/reviews} (UC020):
     * hàng chờ duyệt, đã duyệt, và toàn bộ - ứng với bộ lọc "Chờ duyệt / Đã duyệt /
     * Tất cả". Đều nạp sẵn {@code tour} và {@code user} bằng {@code @EntityGraph}:
     * bảng ở màn quản trị hiện tên tour và người viết ngay trên mỗi dòng, mà
     * {@code open-in-view} đang tắt nên chạm vào hai quan hệ LAZY đó lúc dựng
     * khuôn mẫu sẽ cắt cụt trang giữa chừng (gotcha #43).
     */
    @EntityGraph(attributePaths = {"tour", "user"})
    Page<Review> findByApprovedFalseOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"tour", "user"})
    Page<Review> findByApprovedTrueOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"tour", "user"})
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Review> findByUserIdAndTourId(Long userId, Long tourId);

    /** Chặn quy tắc "mỗi tài khoản đánh giá một tour một lần". */
    boolean existsByUserIdAndTourId(Long userId, Long tourId);

    long countByTourIdAndApprovedTrue(Long tourId);

    long countByApprovedFalse();

    /** Hai truy vấn đếm dưới đây phục vụ quy tắc chặn xoá tour và tài khoản. */
    long countByTourId(Long tourId);

    long countByUserId(Long userId);

    /**
     * Điểm trung bình của tour, chỉ tính đánh giá đã duyệt.
     *
     * <p>Trả về null khi tour chưa có đánh giá nào - bên gọi hiển thị "chưa có
     * đánh giá" thay vì vẽ 0 sao, vốn trông như bị chê.</p>
     */
    @Query("SELECT AVG(CAST(r.rating AS double)) FROM Review r "
            + "WHERE r.tour.id = :tourId AND r.approved = true")
    Double averageRatingByTourId(@Param("tourId") Long tourId);
}
