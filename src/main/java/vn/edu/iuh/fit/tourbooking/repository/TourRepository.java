package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.Tour;

import java.util.List;
import java.util.Optional;

@Repository
// Truy vấn tour: tìm kiếm có lọc động (Specification), gợi ý, thống kê theo danh mục.
public interface TourRepository extends JpaRepository<Tour, Long>, JpaSpecificationExecutor<Tour> {

    Optional<Tour> findBySlug(String slug);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countByCategoryId(Long categoryId);

    List<Tour> findByCategoryIdOrderByNameAsc(Long categoryId);

    @EntityGraph(attributePaths = "category")
    List<Tour> findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Tour> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    @Query("""
            SELECT DISTINCT t FROM Tour t
            LEFT JOIN FETCH t.images
            JOIN FETCH t.category
            WHERE t.id = :id
            """)
    Optional<Tour> findDetailById(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Tour t SET t.viewCount = t.viewCount + 1 WHERE t.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("""
            SELECT t FROM Tour t
            WHERE t.active = true AND t.searchText LIKE %:keyword%
            ORDER BY t.viewCount DESC
            """)
    List<Tour> suggest(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT t.destination FROM Tour t WHERE t.active = true ORDER BY t.destination")
    List<String> findAllDestinations();

    @Query("""
            SELECT t.category.id, COUNT(t) FROM Tour t
            WHERE t.active = true
            GROUP BY t.category.id
            """)
    List<Object[]> countActiveGroupedByCategory();

    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT t FROM Tour t
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR t.searchText LIKE %:keyword%
                   OR LOWER(t.code) LIKE %:keyword%)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
            """)
    Page<Tour> adminSearch(@Param("keyword") String keyword,
                           @Param("categoryId") Long categoryId,
                           Pageable pageable);
}
