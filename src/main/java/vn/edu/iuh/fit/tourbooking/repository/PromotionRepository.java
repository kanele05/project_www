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

@Repository
// Truy vấn mã khuyến mãi, kèm bản khoá bi quan dùng khi ghi/trả lượt dùng.
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    Page<Promotion> findAllByOrderByIdDesc(Pageable pageable);

    long countByActiveTrue();

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

    @Query("SELECT p FROM Promotion p WHERE p.active = true "
            + "AND p.startAt <= :now AND p.endAt >= :now "
            + "AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit) "
            + "ORDER BY p.endAt ASC")
    List<Promotion> findRunning(@Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.id = :id")
    Optional<Promotion> findByIdForUpdate(@Param("id") Long id);
}
