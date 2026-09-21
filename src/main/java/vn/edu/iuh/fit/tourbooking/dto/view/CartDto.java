package vn.edu.iuh.fit.tourbooking.dto.view;

import java.math.BigDecimal;
import java.util.List;

/**
 * Toàn bộ giỏ hàng của phiên hiện tại.
 *
 * <p>Mọi thao tác giỏ hàng qua AJAX đều trả về <b>nguyên trạng thái mới của cả
 * giỏ</b> chứ không chỉ dòng vừa đổi. Nhờ vậy huy hiệu trên thanh điều hướng,
 * tổng tiền và danh sách dòng luôn khớp nhau; nếu chỉ trả về phần thay đổi thì
 * phía trình duyệt phải tự cộng trừ và sẽ lệch dần sau vài thao tác.</p>
 *
 * @param message câu thông báo đã tra sẵn từ {@code messages.properties} theo
 *                ngôn ngữ hiện tại, để hiện lên khung nhắc; {@code null} khi chỉ
 *                đọc giỏ hàng
 */
public record CartDto(List<CartItemDto> items,
                      int itemCount,
                      int totalQuantity,
                      BigDecimal totalAmount,
                      String message) {

    /** Bản sao của giỏ này kèm một câu thông báo - dùng sau mỗi thao tác ghi. */
    public CartDto withMessage(String newMessage) {
        return new CartDto(items, itemCount, totalQuantity, totalAmount, newMessage);
    }
}
