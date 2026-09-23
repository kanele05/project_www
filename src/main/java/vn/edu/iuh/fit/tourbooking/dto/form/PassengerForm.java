package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.edu.iuh.fit.tourbooking.entity.Gender;

import java.time.LocalDate;

/**
 * Một hành khách nhập ở bước thanh toán (mục 12.7).
 *
 * <p>Chỉ <b>họ tên</b> là bắt buộc - các cột còn lại của
 * {@code booking_passengers} đều để trống được. <b>Loại hành khách (người lớn /
 * trẻ em) không nằm ở đây</b>: vị trí trong {@link PassengerGroupForm#getAdults()}
 * hay {@link PassengerGroupForm#getChildren()} mới quyết định loại, để người dùng
 * không tự chọn được loại khác với số người lớn/trẻ em đã đặt ở giỏ hàng.</p>
 */
@Data
public class PassengerForm {

    @NotBlank(message = "{validation.fullName.required}")
    @Size(max = 100, message = "{validation.fullName.size}")
    private String fullName;

    private Gender gender;

    /** Không được ở tương lai - kiểm lại lần nữa ở service (mục 12.7, bước 2). */
    @PastOrPresent(message = "{validation.passenger.birthDate.future}")
    private LocalDate birthDate;

    @Size(max = 30, message = "{validation.passenger.idNumber.size}")
    private String idNumber;

    @Size(max = 20, message = "{validation.passenger.phone.size}")
    private String phone;

    private boolean singleRoom;

    @Size(max = 255, message = "{validation.passenger.note.size}")
    private String note;
}
