package vn.edu.iuh.fit.tourbooking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kiểm tra chuỗi hình thức thanh toán gửi lên có khớp một trong ba lựa chọn
 * hợp lệ của {@link vn.edu.iuh.fit.tourbooking.entity.PaymentMethod} không.
 *
 * <p>{@code checkout.html} gửi lên câu chữ đã dịch (ví dụ "Thanh toán tại văn
 * phòng"/"Pay at our office") chứ không gửi mã enum - xem lý do ở Javadoc của
 * {@link vn.edu.iuh.fit.tourbooking.entity.PaymentMethod}. Trước bản vá này,
 * {@code CheckoutForm.paymentMethod} chỉ có {@code @NotBlank}: một request tự
 * chế gửi một chuỗi dài hơn cột {@code bookings.payment_method} (30 ký tự) sẽ
 * qua được validate rồi vỡ ở lúc ghi CSDL (lỗi cắt chuỗi, trang 500). Bằng cách
 * so khớp với đúng ba câu hợp lệ (cả bản tiếng Việt lẫn tiếng Anh, không phụ
 * thuộc ngôn ngữ hiện tại của request), một giá trị tự chế bất kỳ - dài hay
 * ngắn - đều bị chặn ngay ở vòng validate với một câu thông báo tử tế.</p>
 */
@Documented
@Constraint(validatedBy = ValidPaymentMethodValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPaymentMethod {

    String message() default "{validation.paymentMethod.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
