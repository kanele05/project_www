package vn.edu.iuh.fit.tourbooking.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.dto.view.TourDetailDto;
import vn.edu.iuh.fit.tourbooking.dto.view.TourSummaryDto;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.entity.TourImage;

import java.util.List;

@Component
@RequiredArgsConstructor
// Chuyển Tour sang các DTO hiển thị (tóm tắt/gợi ý/chi tiết), ghép URL ảnh.
public class TourMapper {

    private final ViewUrls urls;

    public TourSummaryDto toSummary(Tour tour) {
        return build(tour, tour.getCategory().getId(), tour.getCategory().getName());
    }

    public TourSummaryDto toSuggestion(Tour tour) {
        return build(tour, null, null);
    }

    public TourDetailDto toDetail(Tour tour) {
        List<String> imageUrls = tour.getImages().stream()
                .map(TourImage::getImagePath)
                .map(urls::image)
                .toList();

        return new TourDetailDto(
                toSummary(tour),
                tour.getDescription(),
                tour.getItinerary(),
                tour.isActive(),
                imageUrls);
    }

    private TourSummaryDto build(Tour tour, Long categoryId, String categoryName) {
        return new TourSummaryDto(
                tour.getId(),
                tour.getCode(),
                tour.getName(),
                tour.getSlug(),
                tour.getShortDescription(),
                tour.getDestination(),
                tour.getDepartureLocation(),
                tour.getDurationDays(),
                tour.getDurationNights(),
                tour.getBasePrice(),
                tour.getTransportation(),
                urls.image(tour.getThumbnail()),
                urls.tourDetail(tour.getId(), tour.getSlug()),
                categoryId,
                categoryName,
                tour.isFeatured(),
                tour.getViewCount());
    }
}
