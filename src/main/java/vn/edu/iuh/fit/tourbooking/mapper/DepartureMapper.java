package vn.edu.iuh.fit.tourbooking.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.config.AppProperties;
import vn.edu.iuh.fit.tourbooking.dto.view.DepartureDto;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
// Chuyển TourDeparture sang DTO hiển thị cho ô chọn ngày AJAX, kèm cờ isBookable đã tính theo hạn chót.
public class DepartureMapper {

    private final AppProperties appProperties;

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
                departure.isBookable(appProperties.booking().cutoffDays()));
    }

    private String format(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMAT);
    }
}
