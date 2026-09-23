package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
// Biểu mẫu thêm/sửa đợt khởi hành ở khu quản trị.
public class DepartureForm {

    private Long id;

    private Long tourId;

    @NotNull(message = "{validation.departure.departureDate.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;

    @NotNull(message = "{validation.departure.returnDate.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate returnDate;

    @NotNull(message = "{validation.departure.totalSeats.required}")
    @Min(value = 1, message = "{validation.departure.totalSeats.min}")
    private Integer totalSeats;

    @NotNull(message = "{validation.departure.priceAdult.required}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{validation.departure.priceAdult.min}")
    private BigDecimal priceAdult;

    @NotNull(message = "{validation.departure.priceChild.required}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{validation.departure.priceChild.min}")
    private BigDecimal priceChild;

    private boolean active = true;

    public static DepartureForm from(TourDeparture departure) {
        DepartureForm form = new DepartureForm();
        form.setId(departure.getId());
        form.setTourId(departure.getTour().getId());
        form.setDepartureDate(departure.getDepartureDate());
        form.setReturnDate(departure.getReturnDate());
        form.setTotalSeats(departure.getTotalSeats());
        form.setPriceAdult(departure.getPriceAdult());
        form.setPriceChild(departure.getPriceChild());
        form.setActive(departure.isActive());
        return form;
    }
}
