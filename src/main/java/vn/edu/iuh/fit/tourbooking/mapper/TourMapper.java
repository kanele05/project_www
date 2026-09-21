package vn.edu.iuh.fit.tourbooking.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.dto.view.TourDetailDto;
import vn.edu.iuh.fit.tourbooking.dto.view.TourSummaryDto;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.entity.TourImage;

import java.util.List;

/**
 * Đổi entity {@link Tour} sang DTO cho web service.
 *
 * <p><b>Cạm bẫy phải nhớ:</b> {@code open-in-view} đang tắt, nên mọi phương thức
 * ở đây chỉ được đụng vào những quan hệ mà nơi gọi đã nạp sẵn.</p>
 * <ul>
 *   <li>{@link #toSummary(Tour)} có đọc {@code tour.category} &rarr; nơi gọi phải
 *       dùng truy vấn có nạp kèm danh mục ({@code TourSpecifications.fetchCategory}
 *       hoặc {@code @EntityGraph}).</li>
 *   <li>{@link #toSuggestion(Tour)} <b>không</b> đọc danh mục, dùng được với
 *       {@code TourRepository.suggest} vốn không nạp kèm gì cả.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TourMapper {

    private final ViewUrls urls;

    /** Bản rút gọn - cần danh mục đã được nạp sẵn. */
    public TourSummaryDto toSummary(Tour tour) {
        return build(tour, tour.getCategory().getId(), tour.getCategory().getName());
    }

    /**
     * Bản rút gọn cho ô gợi ý tìm kiếm, bỏ trống thông tin danh mục.
     *
     * <p>Truy vấn gợi ý cố tình không nạp kèm danh mục vì danh sách gợi ý không
     * hiển thị nó; gọi {@link #toSummary(Tour)} ở đó sẽ ném
     * {@code LazyInitializationException}.</p>
     */
    public TourSummaryDto toSuggestion(Tour tour) {
        return build(tour, null, null);
    }

    /** Bản đầy đủ - cần cả danh mục lẫn bộ ảnh đã được nạp sẵn. */
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
