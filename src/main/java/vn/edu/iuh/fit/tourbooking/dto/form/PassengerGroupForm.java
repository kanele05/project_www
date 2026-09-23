package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Danh sách hành khách của <b>một dòng giỏ hàng</b> (một đợt khởi hành), dùng ở
 * bước thanh toán (mục 12.7).
 *
 * <p>{@code departureId} chỉ để đối chiếu ngược lại đúng dòng giỏ hàng nào ở tầng
 * controller/service - <b>không</b> dùng để tra giá hay số chỗ, những con số đó
 * vẫn luôn được đọc lại từ CSDL như từ trước tới nay.</p>
 *
 * <p>Tách hai danh sách {@link #adults} / {@link #children} thay vì một danh
 * sách kèm trường "loại" là chủ đích: người dùng không có ô nào để chọn loại
 * khách, độ dài của từng danh sách phải khớp {@code numAdults}/{@code numChildren}
 * của dòng giỏ hàng tương ứng - sai lệch bị chặn ở cả hai tầng validate + service
 * (xem {@code BookingService.placeOrder}).</p>
 */
@Data
public class PassengerGroupForm {

    private Long departureId;

    @Valid
    private List<PassengerForm> adults = new ArrayList<>();

    @Valid
    private List<PassengerForm> children = new ArrayList<>();
}
