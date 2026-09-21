package vn.edu.iuh.fit.tourbooking.repository.spec;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.iuh.fit.tourbooking.dto.form.TourSearchForm;
import vn.edu.iuh.fit.tourbooking.entity.Tour;

import java.math.BigDecimal;

/**
 * Các mảnh điều kiện lọc tour, ghép động lại với nhau bằng {@code Specification}.
 *
 * <p>Trang danh sách cho phép kết hợp tuỳ ý từ khoá, danh mục, điểm đến và
 * khoảng giá. Nếu dùng truy vấn dẫn xuất thì phải viết
 * {@code findByActiveTrueAndCategoryIdAndBasePriceBetween...} cho từng tổ hợp -
 * mười mấy phương thức gần giống nhau. Cách này chỉ cần ghép các mảnh nhỏ.</p>
 */
public final class TourSpecifications {

    private TourSpecifications() {
    }

    /** Ghép toàn bộ điều kiện lọc từ biểu mẫu tìm kiếm. */
    public static Specification<Tour> from(TourSearchForm form) {
        return Specification.allOf(
                fetchCategory(),
                isActive(),
                keyword(form.normalizedKeyword()),
                categoryId(form.getCategoryId()),
                destination(form.getDestination()),
                priceFrom(form.getMinPrice()),
                priceTo(form.getMaxPrice()));
    }

    /** Trang công khai chỉ hiện tour đang mở bán. */
    public static Specification<Tour> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    /**
     * Tìm không dấu.
     *
     * <p>So trên cột {@code searchText} (đã bỏ dấu, chữ thường) chứ không so trên
     * {@code name}: collation {@code Vietnamese_CI_AI} của CSDL bỏ được dấu thanh
     * nhưng không coi {@code Đ} là {@code D}, nên tìm thẳng trên {@code name} thì
     * gõ "da lat" sẽ không ra "Đà Lạt". Từ khoá đầu vào phải được
     * {@code TourSearchForm.normalizedKeyword()} chuẩn hoá trước.</p>
     */
    public static Specification<Tour> keyword(String normalizedKeyword) {
        if (normalizedKeyword == null || normalizedKeyword.isBlank()) {
            return null;
        }
        String pattern = "%" + normalizedKeyword + "%";
        return (root, query, cb) -> cb.like(root.get("searchText"), pattern);
    }

    public static Specification<Tour> categoryId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Tour> destination(String destination) {
        if (destination == null || destination.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("destination"), destination);
    }

    public static Specification<Tour> priceFrom(BigDecimal min) {
        if (min == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("basePrice"), min);
    }

    public static Specification<Tour> priceTo(BigDecimal max) {
        if (max == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("basePrice"), max);
    }

    /**
     * Nạp kèm danh mục ngay trong câu truy vấn chính để tránh bẫy N+1: thẻ tour
     * nào cũng hiện tên danh mục, không nạp sẵn thì 12 tour trên trang sẽ sinh
     * thêm 12 câu truy vấn (và với {@code open-in-view = false} thì còn tệ hơn -
     * template ném thẳng {@code LazyInitializationException}).
     *
     * <p><b>Bắt buộc phải bỏ qua câu đếm.</b> {@code Page} chạy thêm một truy vấn
     * {@code count} và Hibernate không cho {@code fetch} trong đó
     * ("query specified join fetching, but the owner of the fetched association
     * was not present"). Vì vậy phải xem kiểu kết quả: chỉ fetch khi đang chạy
     * truy vấn lấy dữ liệu. Việc fetch quan hệ {@code @ManyToOne} như thế này an
     * toàn với phân trang - chỉ có fetch collection mới khiến Hibernate phải
     * phân trang trong bộ nhớ.</p>
     */
    public static Specification<Tour> fetchCategory() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()
                    && long.class != query.getResultType()) {
                root.fetch("category", JoinType.INNER);
            }
            return null;   // không thêm điều kiện lọc nào
        };
    }
}
