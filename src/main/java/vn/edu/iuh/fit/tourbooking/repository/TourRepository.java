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

/**
 * Truy vấn tour.
 *
 * <p>Kế thừa {@code JpaSpecificationExecutor} để trang danh sách ghép động các
 * điều kiện lọc (từ khoá, danh mục, khoảng giá, điểm đến) mà không phải viết
 * hàng chục phương thức {@code findByXAndYAndZ}.</p>
 */
@Repository
public interface TourRepository extends JpaRepository<Tour, Long>, JpaSpecificationExecutor<Tour> {

    Optional<Tour> findBySlug(String slug);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * <b>Truy vấn chặn xoá danh mục.</b> Đề bài yêu cầu ràng buộc phải được kiểm
     * tra trong chương trình chứ không dựa vào ràng buộc của hệ quản trị CSDL.
     */
    long countByCategoryId(Long categoryId);

    /**
     * Toàn bộ tour của một danh mục cho trang chi tiết CHỈ XEM của quản trị
     * viên (mục 12.8) - khác {@link #findByCategoryIdAndActiveTrue}: quản trị
     * viên phải thấy cả tour đã ngừng bán.
     */
    List<Tour> findByCategoryIdOrderByNameAsc(Long categoryId);

    /**
     * Trang chủ: tour nổi bật. Nạp kèm danh mục bằng {@code @EntityGraph} vì
     * {@code open-in-view} đang tắt - template truy cập {@code tour.category.name}
     * mà chưa nạp sẽ ném {@code LazyInitializationException}.
     */
    @EntityGraph(attributePaths = "category")
    List<Tour> findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Tour> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    /**
     * Nạp một tour kèm danh mục và bộ ảnh cho trang chi tiết.
     *
     * <p>Chỉ {@code join fetch} <b>một</b> collection trong một câu truy vấn:
     * lấy đồng thời cả ảnh lẫn các đợt khởi hành sẽ tạo tích Descartes, số dòng
     * trả về bằng tích số lượng hai bên. Các đợt khởi hành được nạp riêng bằng
     * {@code TourDepartureRepository}.</p>
     */
    @Query("""
            SELECT DISTINCT t FROM Tour t
            LEFT JOIN FETCH t.images
            JOIN FETCH t.category
            WHERE t.id = :id
            """)
    Optional<Tour> findDetailById(@Param("id") Long id);

    /**
     * Tăng lượt xem bằng một câu UPDATE thay vì nạp entity rồi ghi lại: tránh
     * ghi đè các trường khác nếu có người đang sửa tour cùng lúc.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Tour t SET t.viewCount = t.viewCount + 1 WHERE t.id = :id")
    void incrementViewCount(@Param("id") Long id);

    /**
     * Gợi ý cho ô tìm kiếm AJAX ({@code /api/tours/suggest}).
     *
     * <p>Tìm trên cột {@code searchText} đã bỏ dấu chứ không tìm thẳng trên
     * {@code name}: xem chú thích ở {@code Tour.searchText} về việc collation
     * {@code Vietnamese_CI_AI} không đồng nhất {@code Đ} với {@code D}.
     * Nơi gọi phải chuẩn hoá từ khoá bằng
     * {@code SlugUtil.removeDiacritics(...).toLowerCase()} trước khi truyền vào.</p>
     */
    @Query("""
            SELECT t FROM Tour t
            WHERE t.active = true AND t.searchText LIKE %:keyword%
            ORDER BY t.viewCount DESC
            """)
    List<Tour> suggest(@Param("keyword") String keyword, Pageable pageable);

    /** Các điểm đến đang có tour - đổ vào ô lọc ở trang danh sách. */
    @Query("SELECT DISTINCT t.destination FROM Tour t WHERE t.active = true ORDER BY t.destination")
    List<String> findAllDestinations();

    /**
     * Số tour đang bán của từng danh mục, trả về từng dòng {@code [categoryId, count]}.
     *
     * <p>Trang chủ hiện "12 hành trình" ngay trên mỗi ô danh mục. Đếm bằng
     * <b>một</b> câu gộp nhóm chứ không lặp {@code countByCategoryId} cho từng
     * danh mục: cách kia là bẫy N+1 kinh điển, sáu danh mục thành sáu lượt đi về
     * cơ sở dữ liệu cho một khối duy nhất trên trang.</p>
     */
    @Query("""
            SELECT t.category.id, COUNT(t) FROM Tour t
            WHERE t.active = true
            GROUP BY t.category.id
            """)
    List<Object[]> countActiveGroupedByCategory();

    /**
     * Tìm kiếm ở màn quản trị.
     *
     * <p>Khác truy vấn của trang công khai ở hai điểm: <b>có cả tour đã ngừng
     * bán</b> (quản trị viên phải sửa được chúng), và tìm cả trên mã tour.</p>
     *
     * <p>Dùng {@code @EntityGraph} thay vì {@code JOIN FETCH} viết tay: với
     * {@code Pageable}, Spring Data phải tự sinh thêm một câu đếm, mà nó không
     * sinh được câu đếm từ truy vấn có {@code JOIN FETCH}.</p>
     */
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
