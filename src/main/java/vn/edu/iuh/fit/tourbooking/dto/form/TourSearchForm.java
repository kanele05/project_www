package vn.edu.iuh.fit.tourbooking.dto.form;

import lombok.Data;
import org.springframework.data.domain.Sort;
import vn.edu.iuh.fit.tourbooking.util.SlugUtil;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Data
// Điều kiện lọc/tìm kiếm tour ở trang danh sách công khai (từ khoá đã bỏ dấu, danh mục, điểm đến, khoảng giá, sắp xếp).
public class TourSearchForm {

    private String q;

    private Long categoryId;
    private String destination;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private String sort = "newest";

    public String normalizedKeyword() {
        if (q == null || q.isBlank()) {
            return null;
        }
        return SlugUtil.removeDiacritics(q.trim()).toLowerCase(Locale.ROOT);
    }

    public boolean hasKeyword() {
        return normalizedKeyword() != null;
    }

    public boolean hasAnyFilter() {
        return hasKeyword() || categoryId != null
                || (destination != null && !destination.isBlank())
                || minPrice != null || maxPrice != null;
    }

    // Suy ra Sort của Spring Data từ tham số "sort" trên URL.
    public Sort toSort() {
        return switch (sort == null ? "newest" : sort) {
            case "price-asc"  -> Sort.by(Sort.Direction.ASC, "basePrice");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "basePrice");
            case "popular"    -> Sort.by(Sort.Direction.DESC, "viewCount");
            default           -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    // Dựng lại query string từ bộ lọc hiện tại, dùng để giữ nguyên bộ lọc khi đổi trang/đổi ngôn ngữ.
    public String toQueryPrefix(String path) {
        List<String> parts = new ArrayList<>();
        addParam(parts, "q", q);
        addParam(parts, "categoryId", categoryId);
        addParam(parts, "destination", destination);
        addParam(parts, "minPrice", minPrice);
        addParam(parts, "maxPrice", maxPrice);
        addParam(parts, "sort", sort);

        StringBuilder sb = new StringBuilder(path).append('?');
        if (!parts.isEmpty()) {
            sb.append(String.join("&", parts)).append('&');
        }
        return sb.toString();
    }

    private void addParam(List<String> parts, String name, Object value) {
        if (value == null || value.toString().isBlank()) {
            return;
        }
        parts.add(name + "=" + URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
    }
}
