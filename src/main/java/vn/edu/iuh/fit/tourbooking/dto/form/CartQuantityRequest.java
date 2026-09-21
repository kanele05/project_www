package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Thân yêu cầu khi <b>sửa số khách</b> của một dòng giỏ hàng.
 *
 * <p><b>Vì sao tách khỏi {@link CartItemRequest} thay vì dùng chung:</b> hai
 * thao tác có đầu vào khác nhau thật sự. Lúc thêm, phía gọi phải cho biết thêm
 * <i>cái gì</i> vào giỏ; lúc sửa, dòng cần sửa đã nằm ngay trên đường dẫn
 * ({@code PUT /api/cart/items/{departureId}}) nên thân yêu cầu chỉ còn số khách.
 * Dùng chung một lớp thì ràng buộc "phải có tourId hoặc departureId" của thao
 * tác thêm sẽ chặn nhầm thao tác sửa - đúng lỗi đã vấp phải khi kiểm thử.</p>
 *
 * <p>Cho phép số khách bằng 0: đó chính là cách đề bài yêu cầu để xoá một dòng
 * khỏi giỏ.</p>
 */
@Data
public class CartQuantityRequest {

    @Min(value = 0, message = "{validation.cart.numAdults.min}")
    @Max(value = 30, message = "{validation.cart.numAdults.max}")
    private int numAdults;

    @Min(value = 0, message = "{validation.cart.numChildren.min}")
    @Max(value = 30, message = "{validation.cart.numChildren.max}")
    private int numChildren;
}
