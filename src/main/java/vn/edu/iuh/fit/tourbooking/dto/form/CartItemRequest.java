package vn.edu.iuh.fit.tourbooking.dto.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
// Payload REST khi thêm vào giỏ (POST): phải có đúng một trong departureId/tourId.
public class CartItemRequest {

    private Long departureId;

    private Long tourId;

    @Min(value = 0, message = "{validation.cart.numAdults.min}")
    @Max(value = 30, message = "{validation.cart.numAdults.max}")
    private int numAdults = 1;

    @Min(value = 0, message = "{validation.cart.numChildren.min}")
    @Max(value = 30, message = "{validation.cart.numChildren.max}")
    private int numChildren = 0;

    @JsonIgnore
    @AssertTrue(message = "{validation.cart.target.required}")
    // Phải chọn tour hoặc đợt khởi hành cụ thể để thêm vào giỏ.
    public boolean isTargetProvided() {
        return departureId != null || tourId != null;
    }
}
