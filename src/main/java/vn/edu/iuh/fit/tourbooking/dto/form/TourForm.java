package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.tourbooking.entity.Tour;

import java.math.BigDecimal;
import java.util.List;

@Data
// Biểu mẫu thêm/sửa tour ở khu quản trị, kèm ảnh đại diện và thư viện ảnh.
public class TourForm {

    private Long id;

    @NotBlank(message = "{validation.tour.code.required}")
    @Size(max = 30, message = "{validation.tour.code.size}")
    private String code;

    @NotBlank(message = "{validation.tour.name.required}")
    @Size(max = 200, message = "{validation.tour.name.size}")
    private String name;

    @Size(max = 500, message = "{validation.tour.shortDescription.size}")
    private String shortDescription;

    private String description;

    private String itinerary;

    @NotBlank(message = "{validation.tour.departureLocation.required}")
    @Size(max = 100, message = "{validation.tour.departureLocation.size}")
    private String departureLocation;

    @NotBlank(message = "{validation.tour.destination.required}")
    @Size(max = 100, message = "{validation.tour.destination.size}")
    private String destination;

    @NotNull(message = "{validation.tour.durationDays.required}")
    @Min(value = 1, message = "{validation.tour.durationDays.min}")
    @Max(value = 60, message = "{validation.tour.durationDays.max}")
    private Integer durationDays;

    @NotNull(message = "{validation.tour.durationNights.required}")
    @Min(value = 0, message = "{validation.tour.durationNights.min}")
    @Max(value = 60, message = "{validation.tour.durationNights.max}")
    private Integer durationNights;

    @NotNull(message = "{validation.tour.basePrice.required}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{validation.tour.basePrice.min}")
    @Digits(integer = 13, fraction = 2, message = "{validation.tour.basePrice.digits}")
    private BigDecimal basePrice;

    @Size(max = 100, message = "{validation.tour.transportation.size}")
    private String transportation;

    private boolean featured;

    private boolean active = true;

    @NotNull(message = "{validation.tour.category.required}")
    private Long categoryId;

    private MultipartFile thumbnailFile;

    private List<MultipartFile> galleryFiles;

    private String currentThumbnail;

    public static TourForm from(Tour tour) {
        TourForm form = new TourForm();
        form.setId(tour.getId());
        form.setCode(tour.getCode());
        form.setName(tour.getName());
        form.setShortDescription(tour.getShortDescription());
        form.setDescription(tour.getDescription());
        form.setItinerary(tour.getItinerary());
        form.setDepartureLocation(tour.getDepartureLocation());
        form.setDestination(tour.getDestination());
        form.setDurationDays(tour.getDurationDays());
        form.setDurationNights(tour.getDurationNights());
        form.setBasePrice(tour.getBasePrice());
        form.setTransportation(tour.getTransportation());
        form.setFeatured(tour.isFeatured());
        form.setActive(tour.isActive());
        form.setCategoryId(tour.getCategory().getId());
        form.setCurrentThumbnail(tour.getThumbnail());
        return form;
    }
}
