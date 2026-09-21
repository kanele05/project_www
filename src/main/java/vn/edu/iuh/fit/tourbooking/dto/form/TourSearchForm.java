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

/**
 * Gom toàn bộ tham số lọc của trang danh sách tour vào một đối tượng.
 *
 * <p>Spring MVC tự gán các tham số trên URL vào đây qua {@code @ModelAttribute},
 * nhờ vậy chữ ký của controller không phình thành sáu, bảy tham số. Đây là DTO
 * nên được phép dùng {@code @Data} (khác với entity - xem quy ước ở README).</p>
 */
@Data
public class TourSearchForm {

    /** Từ khoá người dùng gõ, có dấu hay không dấu đều được. */
    private String q;

    private Long categoryId;
    private String destination;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    /** newest | price-asc | price-desc | popular */
    private String sort = "newest";

    /**
     * Từ khoá đã chuẩn hoá để so với cột {@code tours.search_text}.
     *
     * <p>Bắt buộc phải bỏ dấu và hạ chữ thường ở đây, vì cột kia lưu dạng đã bỏ
     * dấu. Nếu truyền thẳng chuỗi có dấu xuống thì tìm gì cũng không ra.</p>
     *
     * @return null nếu người dùng không nhập gì
     */
    public String normalizedKeyword() {
        if (q == null || q.isBlank()) {
            return null;
        }
        return SlugUtil.removeDiacritics(q.trim()).toLowerCase(Locale.ROOT);
    }

    public boolean hasKeyword() {
        return normalizedKeyword() != null;
    }

    /** Người dùng có đang lọc gì không - dùng để hiện nút "Xoá bộ lọc". */
    public boolean hasAnyFilter() {
        return hasKeyword() || categoryId != null
                || (destination != null && !destination.isBlank())
                || minPrice != null || maxPrice != null;
    }

    /** Chuyển lựa chọn sắp xếp trên giao diện thành {@link Sort} của Spring Data. */
    public Sort toSort() {
        return switch (sort == null ? "newest" : sort) {
            case "price-asc"  -> Sort.by(Sort.Direction.ASC, "basePrice");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "basePrice");
            case "popular"    -> Sort.by(Sort.Direction.DESC, "viewCount");
            default           -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    /**
     * Dựng phần đầu của đường dẫn phân trang, đã kèm mọi tham số lọc hiện tại
     * <b>trừ</b> {@code page}. Nơi gọi chỉ việc nối thêm {@code page=N}.
     *
     * <p>Nhờ vậy khi bấm sang trang 2, các bộ lọc và từ khoá vẫn được giữ nguyên -
     * đây đúng là chỗ hay hỏng nếu ghép chuỗi cẩu thả. Giá trị được mã hoá bằng
     * {@link URLEncoder} nên từ khoá tiếng Việt có dấu cũng không làm vỡ URL.</p>
     *
     * @param path phần đường dẫn, ví dụ {@code /tours}
     * @return ví dụ {@code /tours?q=hu%E1%BA%BF&categoryId=3&}
     */
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
