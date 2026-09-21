package vn.edu.iuh.fit.tourbooking.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Một dòng giỏ hàng trả về cho AJAX.
 *
 * <p>Trùng phần lớn trường với {@code session.CartItem} nhưng vẫn tách riêng:
 * {@code CartItem} là thứ nằm trong {@code HttpSession} và có thể phải thay đổi
 * vì lý do nội bộ, còn lớp này là hợp đồng đã công bố với phía trình duyệt. Gộp
 * hai vai trò vào một lớp thì mỗi lần sửa giỏ hàng lại có nguy cơ làm hỏng AJAX.</p>
 *
 * @param quantity tổng số khách của dòng - chính là "số lượng" theo yêu cầu
 *                 "số lượng bằng 0 thì xoá khỏi giỏ" của đề bài
 */
public record CartItemDto(Long departureId,
                          Long tourId,
                          String tourName,
                          String tourSlug,
                          String thumbnailUrl,
                          String detailUrl,
                          LocalDate departureDate,
                          String departureDateText,
                          int numAdults,
                          int numChildren,
                          int quantity,
                          BigDecimal priceAdult,
                          BigDecimal priceChild,
                          BigDecimal subtotal) {
}
