package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Biểu mẫu thông tin liên hệ khi đặt tour.
 *
 * <p>Được điền sẵn từ hồ sơ người dùng nhưng vẫn cho sửa: người đặt tour không
 * nhất thiết là người đi, và số điện thoại liên hệ trong chuyến đi có thể khác
 * số đăng ký tài khoản. Những giá trị nhập ở đây được chép vào đơn hàng và giữ
 * nguyên mãi về sau.</p>
 *
 * <p>Cố ý <b>không</b> có trường nào liên quan tới tiền: giá và thành tiền được
 * đọc lại từ CSDL lúc đặt. Nếu để tiền đi qua biểu mẫu thì người dùng chỉ cần
 * sửa dữ liệu gửi lên là mua được tour giá 0 đồng.</p>
 */
@Data
public class CheckoutForm {

    @NotBlank(message = "{validation.fullName.required}")
    @Size(max = 100, message = "{validation.fullName.size}")
    private String customerName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 150, message = "{validation.email.size}")
    private String customerEmail;

    @NotBlank(message = "{validation.phone.required}")
    @Pattern(regexp = "^0\\d{9,10}$", message = "{validation.phone.invalid}")
    private String customerPhone;

    @Size(max = 255, message = "{validation.address.size}")
    private String customerAddress;

    @NotBlank(message = "{validation.paymentMethod.required}")
    private String paymentMethod;

    @Size(max = 500, message = "{validation.note.size}")
    private String note;

    /**
     * Mã giảm giá, để trống cũng được.
     *
     * <p>Đây là ngoại lệ duy nhất của quy tắc "không cho tiền đi qua biểu mẫu" nói
     * ở trên, và nó không phá quy tắc: biểu mẫu chỉ mang <b>mã</b>, còn số tiền
     * được giảm do {@code PromotionService} tính lại từ CSDL lúc ghi đơn. Sửa dữ
     * liệu gửi lên chỉ đổi được mã, không đổi được số tiền.</p>
     */
    @Size(max = 30, message = "{validation.coupon.size}")
    private String couponCode;
}
