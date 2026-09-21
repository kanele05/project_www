package vn.edu.iuh.fit.tourbooking.dto.view;

import java.util.List;

/**
 * Tour đầy đủ cho {@code GET /api/tours/{id}}.
 *
 * <p>Gói phần rút gọn lại trong {@code summary} thay vì chép lại mười mấy trường:
 * phía trình duyệt dùng chung một hàm dựng thẻ tour cho cả danh sách lẫn chi tiết.
 * Danh sách đợt khởi hành <b>không</b> nằm ở đây - nó có địa chỉ riêng
 * {@code /api/tours/{id}/departures} vì ô chọn ngày cần nạp lại số chỗ trống mà
 * không phải tải lại toàn bộ mô tả tour.</p>
 */
public record TourDetailDto(TourSummaryDto summary,
                            String description,
                            String itinerary,
                            boolean active,
                            List<String> imageUrls) {
}
