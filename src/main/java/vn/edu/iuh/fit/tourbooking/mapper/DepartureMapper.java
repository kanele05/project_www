package vn.edu.iuh.fit.tourbooking.mapper;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.dto.view.DepartureDto;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Đổi entity {@link TourDeparture} sang DTO.
 *
 * <p><b>Mã số tour được truyền vào từ ngoài chứ không đọc bằng
 * {@code departure.getTour().getId()}.</b> Truy vấn
 * {@code TourDepartureRepository.findBookable} không nạp kèm tour, nên
 * {@code getTour()} trả về một proxy chưa khởi tạo; đụng vào nó sau khi giao dịch
 * đã đóng ({@code open-in-view = false}) là chuyện may rủi. Nơi gọi vốn luôn biết
 * sẵn mã tour vì nó nằm ngay trên đường dẫn {@code /api/tours/{id}/departures},
 * nên chỉ việc đưa xuống là hết rủi ro.</p>
 */
@Component
public class DepartureMapper {

    /**
     * Ngày tháng kiểu Việt Nam.
     *
     * <p>Giữ một khuôn cố định thay vì theo ngôn ngữ đang chọn: {@code 08/09/2026}
     * đọc theo kiểu Anh là ngày 9 tháng 8, theo kiểu Việt là ngày 8 tháng 9 - hai
     * cách hiểu khác hẳn nhau cho cùng một chuỗi. Trường {@code departureDate}
     * dạng ISO đi kèm trong phản hồi mới là giá trị để tính toán; chuỗi này chỉ
     * để hiển thị nhanh.</p>
     */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public DepartureDto toDto(TourDeparture departure, Long tourId) {
        return new DepartureDto(
                departure.getId(),
                tourId,
                departure.getDepartureDate(),
                departure.getReturnDate(),
                format(departure.getDepartureDate()),
                format(departure.getReturnDate()),
                departure.getTotalSeats(),
                departure.getAvailableSeats(),
                departure.getPriceAdult(),
                departure.getPriceChild(),
                departure.isBookable());
    }

    private String format(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMAT);
    }
}
