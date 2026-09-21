package vn.edu.iuh.fit.tourbooking.dto.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Thân yêu cầu khi thêm hoặc sửa một dòng giỏ hàng qua web service.
 *
 * <p>Nhận <b>một trong hai</b> mã số, đúng như hai lối đặt tour của đề bài:
 * {@code departureId} khi khách chọn đích danh ngày đi ở trang chi tiết,
 * {@code tourId} khi khách bấm "Đặt tour" ngay tại trang danh sách và để hệ thống
 * tự chọn đợt gần nhất còn chỗ.</p>
 *
 * <p>Ràng buộc số đặt ở DTO chứ không đặt trên entity - vừa đúng chỗ (đây mới là
 * dữ liệu người dùng gửi lên), vừa tránh việc Hibernate lấy chúng ra sinh CHECK
 * constraint trong CSDL, thứ mà đề bài cấm.</p>
 */
@Data
public class CartItemRequest {

    private Long departureId;

    private Long tourId;

    @Min(value = 0, message = "{validation.cart.numAdults.min}")
    @Max(value = 30, message = "{validation.cart.numAdults.max}")
    private int numAdults = 1;

    @Min(value = 0, message = "{validation.cart.numChildren.min}")
    @Max(value = 30, message = "{validation.cart.numChildren.max}")
    private int numChildren = 0;

    /**
     * Phải có ít nhất một trong hai mã số.
     *
     * <p>Gửi thiếu cả hai là <b>lỗi của phía gọi</b>, nên phải ra <b>400</b> chứ
     * không phải 409 - 409 dành cho yêu cầu viết đúng nhưng xung đột với trạng
     * thái dữ liệu (hết chỗ, danh mục còn tour). Đặt phép kiểm tra ở đây thay vì
     * viết {@code if} trong controller cũng để nó chạy chung một đường với mọi
     * ràng buộc khác và tự có thông điệp dịch được.</p>
     *
     * <p>{@code @JsonIgnore} để Jackson không hiểu nhầm đây là một trường cần đọc
     * từ JSON gửi lên.</p>
     */
    @JsonIgnore
    @AssertTrue(message = "{validation.cart.target.required}")
    public boolean isTargetProvided() {
        return departureId != null || tourId != null;
    }
}
