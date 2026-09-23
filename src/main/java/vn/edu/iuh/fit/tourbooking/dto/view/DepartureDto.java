package vn.edu.iuh.fit.tourbooking.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;

// DTO hiển thị đợt khởi hành, dùng cho ô chọn ngày AJAX ở trang chi tiết tour.
public record DepartureDto(Long id,
                           Long tourId,
                           LocalDate departureDate,
                           LocalDate returnDate,
                           String departureDateText,
                           String returnDateText,
                           Integer totalSeats,
                           Integer availableSeats,
                           BigDecimal priceAdult,
                           BigDecimal priceChild,
                           boolean bookable) {
}
