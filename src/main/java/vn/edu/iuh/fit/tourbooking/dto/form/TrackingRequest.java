package vn.edu.iuh.fit.tourbooking.dto.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import vn.edu.iuh.fit.tourbooking.entity.EventType;

// Một sự kiện do tracker.js gửi lên; userId và sessionId KHÔNG nằm ở đây vì server tự xác định.
public record TrackingRequest(@NotNull EventType type,
                              @Positive Long tourId,
                              @Positive Long departureId,
                              @Size(max = 200) String keyword,
                              @Min(0) @Max(86_400_000) Integer durationMs,
                              @Size(max = 255) String pageUrl) {

    // Mỗi loại sự kiện phải mang đủ trường của nó (VIEW/CLICK cần tour, SEARCH cần từ khoá, TIME_ON_PAGE cần tour và thời lượng).
    @JsonIgnore
    @AssertTrue(message = "{validation.tracking.shape}")
    public boolean isConsistent() {
        if (type == null) {
            return true;
        }
        return switch (type) {
            case VIEW, CLICK, ADD_TO_CART -> tourId != null;
            case TIME_ON_PAGE -> tourId != null && durationMs != null;
            case SEARCH -> keyword != null && !keyword.isBlank();
        };
    }
}
