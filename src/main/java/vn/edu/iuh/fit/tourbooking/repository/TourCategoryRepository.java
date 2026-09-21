package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;

import java.util.List;
import java.util.Optional;

/**
 * Truy vấn danh mục tour.
 */
@Repository
public interface TourCategoryRepository extends JpaRepository<TourCategory, Long> {

    Optional<TourCategory> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** Danh mục hiện trên thanh điều hướng của trang công khai. */
    List<TourCategory> findByActiveTrueOrderByNameAsc();

    @Query("""
            SELECT c FROM TourCategory c
            WHERE :keyword IS NULL OR :keyword = ''
               OR c.name LIKE %:keyword%
               OR c.description LIKE %:keyword%
            """)
    Page<TourCategory> search(@Param("keyword") String keyword, Pageable pageable);
}
