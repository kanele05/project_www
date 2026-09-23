package vn.edu.iuh.fit.tourbooking.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;

// DTO hiển thị một dòng giỏ hàng.
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
