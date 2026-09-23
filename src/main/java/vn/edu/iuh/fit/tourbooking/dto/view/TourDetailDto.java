package vn.edu.iuh.fit.tourbooking.dto.view;

import java.util.List;

// DTO hiển thị chi tiết tour cho REST.
public record TourDetailDto(TourSummaryDto summary,
                            String description,
                            String itinerary,
                            boolean active,
                            List<String> imageUrls) {
}
