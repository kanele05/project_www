package vn.edu.iuh.fit.tourbooking.dto.view;

import java.math.BigDecimal;
import java.util.List;

// DTO hiển thị giỏ hàng cho REST/AJAX.
public record CartDto(List<CartItemDto> items,
                      int itemCount,
                      int totalQuantity,
                      BigDecimal totalAmount,
                      String message) {

    public CartDto withMessage(String newMessage) {
        return new CartDto(items, itemCount, totalQuantity, totalAmount, newMessage);
    }
}
