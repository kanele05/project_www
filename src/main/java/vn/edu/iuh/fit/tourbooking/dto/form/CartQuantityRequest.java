package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
// Payload REST khi sửa số khách của một dòng trong giỏ (PUT).
public class CartQuantityRequest {

    @Min(value = 0, message = "{validation.cart.numAdults.min}")
    @Max(value = 30, message = "{validation.cart.numAdults.max}")
    private int numAdults;

    @Min(value = 0, message = "{validation.cart.numChildren.min}")
    @Max(value = 30, message = "{validation.cart.numChildren.max}")
    private int numChildren;
}
