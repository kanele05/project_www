package vn.edu.iuh.fit.tourbooking.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Một đợt khởi hành, phục vụ ô chọn ngày bằng AJAX.
 *
 * <p>Trả về <b>cả</b> ngày dạng ISO lẫn ngày đã định dạng: dạng ISO để phía
 * trình duyệt so sánh và sắp xếp, dạng đã định dạng để hiển thị. Việc định dạng
 * đặt ở máy chủ vì nó phụ thuộc ngôn ngữ đang chọn, mà ngôn ngữ thì máy chủ mới
 * biết (cookie do {@code CookieLocaleResolver} quản lý).</p>
 *
 * @param bookable còn nhận khách hay không - tính bằng nghiệp vụ ở
 *                 {@code TourDeparture.isBookable()} chứ không để phía trình
 *                 duyệt tự suy từ ngày và số chỗ, tránh hai nơi hiểu khác nhau
 */
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
