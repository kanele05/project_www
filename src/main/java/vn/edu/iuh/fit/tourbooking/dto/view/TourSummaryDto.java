package vn.edu.iuh.fit.tourbooking.dto.view;

import java.math.BigDecimal;

/**
 * Một tour rút gọn - đủ để dựng thẻ tour hoặc một dòng gợi ý tìm kiếm.
 *
 * <p><b>Vì sao phải có DTO thay vì trả thẳng entity {@code Tour}:</b></p>
 * <ul>
 *   <li>{@code Tour} có các quan hệ LAZY ({@code images}, {@code departures});
 *       Jackson đụng vào lúc tuần tự hoá sẽ ném lỗi vì
 *       {@code open-in-view = false}, hoặc tệ hơn là âm thầm bắn thêm hàng chục
 *       câu truy vấn.</li>
 *   <li>Entity còn mang những trường chỉ dùng nội bộ ({@code searchText}, các mốc
 *       thời gian kiểm toán) - không có lý do gì công bố ra ngoài.</li>
 *   <li>Đổi tên một cột trong CSDL sẽ không làm hỏng phía trình duyệt.</li>
 * </ul>
 *
 * @param thumbnailUrl đường dẫn đã ghép sẵn để nhét thẳng vào {@code <img src>},
 *                     tự thay bằng ảnh mặc định khi tour chưa có ảnh
 * @param detailUrl    đường dẫn trang chi tiết - dựng ở máy chủ để quy tắc
 *                     {@code /tours/{id}/{slug}} chỉ tồn tại ở một nơi
 */
public record TourSummaryDto(Long id,
                             String code,
                             String name,
                             String slug,
                             String shortDescription,
                             String destination,
                             String departureLocation,
                             Integer durationDays,
                             Integer durationNights,
                             BigDecimal basePrice,
                             String transportation,
                             String thumbnailUrl,
                             String detailUrl,
                             Long categoryId,
                             String categoryName,
                             boolean featured,
                             Long viewCount) {
}
