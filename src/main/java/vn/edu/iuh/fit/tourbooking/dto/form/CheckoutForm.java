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

@Data
// Biểu mẫu thanh toán: thông tin khách, hình thức thanh toán, mã giảm giá, danh sách hành khách theo từng dòng giỏ.
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
    @Size(max = 30, message = "{validation.paymentMethod.invalid}")
    @ValidPaymentMethod
    private String paymentMethod;

    @Size(max = 500, message = "{validation.note.size}")
    private String note;

    @Size(max = 30, message = "{validation.coupon.size}")
    private String couponCode;

    @Valid
    private List<PassengerGroupForm> passengerGroups = new ArrayList<>();
}
