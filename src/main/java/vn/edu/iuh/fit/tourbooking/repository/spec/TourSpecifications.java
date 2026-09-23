package vn.edu.iuh.fit.tourbooking.repository.spec;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.iuh.fit.tourbooking.dto.form.TourSearchForm;
import vn.edu.iuh.fit.tourbooking.entity.Tour;

import java.math.BigDecimal;

// Ghép các điều kiện lọc tour (từ khoá, danh mục, điểm đến, khoảng giá) thành một Specification động.
public final class TourSpecifications {

    private TourSpecifications() {
    }

    // Gộp toàn bộ điều kiện lọc từ form tìm kiếm thành một Specification duy nhất.
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

    public static Specification<Tour> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

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

    public static Specification<Tour> fetchCategory() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()
                    && long.class != query.getResultType()) {
                root.fetch("category", JoinType.INNER);
            }
            return null;
        };
    }
}
