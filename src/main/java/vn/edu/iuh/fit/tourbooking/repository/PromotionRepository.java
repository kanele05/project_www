package vn.edu.iuh.fit.tourbooking.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.Promotion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Truy vấn chương trình khuyến mãi. */
@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    /**
     * Khách gõ mã kiểu gì cũng tìm ra: mã lưu chữ hoa nhưng so sánh không phân
     * biệt hoa thường.
     */
    Optional<Promotion> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    Page<Promotion> findAllByOrderByIdDesc(Pageable pageable);

    long countByActiveTrue();

    /** Tìm kiếm ở màn quản trị: gõ mã hoặc tên đều ra, lọc thêm được theo trạng thái bật/tắt. */
    @Query("""
            SELECT p FROM Promotion p
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR UPPER(p.code) LIKE UPPER(CONCAT('%', :keyword, '%'))
                   OR p.name LIKE %:keyword%)
              AND (:active IS NULL OR p.active = :active)
            """)
    Page<Promotion> search(@Param("keyword") String keyword,
                           @Param("active") Boolean active,
                           Pageable pageable);

    /**
     * Các mã đang chạy tại thời điểm {@code now}: còn bật, trong khoảng hiệu lực,
     * và chưa hết lượt. Điều kiện "còn lượt" nằm ngay trong câu truy vấn để trang
     * khuyến mãi không quảng cáo những mã đã dùng hết.
     */
    @Query("SELECT p FROM Promotion p WHERE p.active = true "
            + "AND p.startAt <= :now AND p.endAt >= :now "
            + "AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit) "
            + "ORDER BY p.endAt ASC")
    List<Promotion> findRunning(@Param("now") LocalDateTime now);

    /**
     * Khoá bi quan chống lost update trên {@code usedCount}: hai đơn cùng dùng
     * một mã gần như cùng lúc thì giao dịch thứ hai phải chờ giao dịch thứ nhất
     * commit rồi mới đọc lại {@code usedCount}/quota mới nhất - không có khoá này
     * cả hai cùng đọc "còn lượt" rồi cùng ghi đè lên cùng một giá trị.
     *
     * <p>Chọn khoá bi quan thay vì {@code @Version} vì không phải sửa
     * {@code database/02_schema.sql} (khác {@code TourDeparture}, nơi
     * {@code @Version} đã có từ Phase 1) - {@code Promotion} chưa có cột
     * {@code version}, thêm vào giữa chừng sẽ phải chạy lại toàn bộ schema.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.id = :id")
    Optional<Promotion> findByIdForUpdate(@Param("id") Long id);
}
