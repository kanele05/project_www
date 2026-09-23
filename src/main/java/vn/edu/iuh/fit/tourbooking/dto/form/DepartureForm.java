package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Biểu mẫu thêm / sửa một đợt khởi hành của tour.
 *
 * <p>{@code @DateTimeFormat(iso = DATE)} là bắt buộc: ô {@code <input type="date">}
 * gửi lên chuỗi dạng {@code 2026-08-22}, không có chú thích này Spring sẽ không
 * biết cách đổi chuỗi đó thành {@code LocalDate}.</p>
 *
 * <p><b>Cố ý KHÔNG có {@code @Future} trên {@link #departureDate}.</b> Trước bản
 * vá này nó có, và hậu quả là một đợt khởi hành đã qua ngày (còn nguyên trong hệ
 * thống vì đã có khách đặt) không sửa được <b>bất cứ điều gì</b> nữa, kể cả chỉ
 * để chỉnh giá hay số chỗ - {@code @Valid} chặn ngay từ vòng bind tham số, trước
 * khi thân {@code AdminDepartureController.save} kịp chạy. Ràng buộc "phải ở
 * tương lai" giờ chuyển xuống {@code DepartureService.save}, nơi biết được đây là
 * <b>tạo mới</b> hay <b>sửa mà không đổi ngày</b> để chỉ áp dụng đúng lúc cần: tạo
 * mới, hoặc sửa mà đổi sang một ngày khác.</p>
 */
@Data
public class DepartureForm {

    private Long id;

    /** Đợt khởi hành luôn thuộc về một tour; lấy từ đường dẫn, không từ biểu mẫu. */
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
