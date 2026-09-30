package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
// Nhóm hành khách (người lớn + trẻ em) ứng với một dòng giỏ hàng/lịch khởi hành.
public class PassengerGroupForm {

    private Long departureId;

    @Valid
    private List<PassengerForm> adults = new ArrayList<>();

    @Valid
    private List<PassengerForm> children = new ArrayList<>();
}
