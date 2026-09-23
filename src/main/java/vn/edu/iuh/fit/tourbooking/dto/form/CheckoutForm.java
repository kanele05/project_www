package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.edu.iuh.fit.tourbooking.validation.ValidPaymentMethod;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Hình thức thanh toán - giá trị gửi lên là câu chữ đã dịch của một trong ba
     * lựa chọn ở {@code checkout.html}, không phải mã enum thô (xem Javadoc
     * {@link vn.edu.iuh.fit.tourbooking.entity.PaymentMethod}).
     *
     * <p>{@code @Size(max = 30)} khớp đúng giới hạn cột {@code bookings.payment_method}
     * (chốt chặn cuối chống lỗi cắt chuỗi 500 nếu {@code @ValidPaymentMethod} có bị
     * qua mặt bằng cách nào đó), còn {@link ValidPaymentMethod} mới là chốt chặn
     * chính: chỉ nhận đúng ba câu hợp lệ, một chuỗi tự chế bất kỳ đều bị từ chối
     * ngay từ bước validate thay vì lặng lẽ rơi vào nhánh "chuyển khoản" mặc định
     * của {@code PaymentService.resolveMethod}.</p>
     */
    @NotBlank(message = "{validation.paymentMethod.required}")
    @Size(max = 30, message = "{validation.paymentMethod.invalid}")
    @ValidPaymentMethod
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

    /**
     * Danh sách hành khách của từng dòng giỏ hàng (mục 12.7 - UC007).
     *
     * <p>Được dựng sẵn ở {@code CheckoutController.checkoutPage} với đúng số ô
     * theo {@code numAdults}/{@code numChildren} của từng dòng giỏ hàng, và
     * <b>đối chiếu lại</b> với giỏ hàng hiện tại ngay khi biểu mẫu được gửi lên -
     * không tin cấu trúc danh sách này tới từ trình duyệt.</p>
     */
    @Valid
    private List<PassengerGroupForm> passengerGroups = new ArrayList<>();
}
