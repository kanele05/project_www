package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.edu.iuh.fit.tourbooking.entity.Gender;

import java.time.LocalDate;

@Data
// Dữ liệu một hành khách nhập tại trang thanh toán.
public class PassengerForm {

    @NotBlank(message = "{validation.fullName.required}")
    @Size(max = 100, message = "{validation.fullName.size}")
    private String fullName;

    private Gender gender;

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
