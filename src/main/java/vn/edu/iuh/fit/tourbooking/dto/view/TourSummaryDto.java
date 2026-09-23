package vn.edu.iuh.fit.tourbooking.dto.view;

import java.math.BigDecimal;

// DTO hiển thị tóm tắt một tour (dùng cho thẻ tour, gợi ý tìm kiếm).
public record TourSummaryDto(Long id,
                             String code,
                             String name,
                             String slug,
                             String shortDescription,
                             String destination,
                             String departureLocation,
                             Integer durationDays,
                             Integer durationNights,
                             BigDecimal basePrice,
                             String transportation,
                             String thumbnailUrl,
                             String detailUrl,
                             Long categoryId,
                             String categoryName,
                             boolean featured,
                             Long viewCount) {
}
