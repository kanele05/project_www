package vn.edu.iuh.fit.tourbooking.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.tourbooking.dto.form.CartItemRequest;
import vn.edu.iuh.fit.tourbooking.dto.form.CartQuantityRequest;
import vn.edu.iuh.fit.tourbooking.dto.view.CartDto;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.mapper.CartMapper;
import vn.edu.iuh.fit.tourbooking.service.CartService;
import vn.edu.iuh.fit.tourbooking.session.CartItem;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

/**
 * Web service giỏ hàng - để thêm tour vào giỏ mà không phải nạp lại trang.
 *
 * <p><b>Giỏ hàng vẫn nằm nguyên trong {@code HttpSession}.</b> Các địa chỉ này
 * không hề tạo ra một kho chứa thứ hai: chúng gọi đúng {@code CartService} mà
 * trang web đang gọi, nên thao tác bằng AJAX hay bằng biểu mẫu thường đều tác
 * động lên cùng một giỏ. Kiểm chứng được: thêm bằng AJAX rồi mở {@code /cart}
 * bằng đường dẫn thường sẽ thấy đúng dòng vừa thêm.</p>
 *
 * <p>Mọi phương thức đều trả về <b>toàn bộ giỏ sau thao tác</b> để giao diện chỉ
 * việc vẽ lại; xem lý do ở {@code CartDto}.</p>
 *
 * <p>Lỗi nghiệp vụ (hết chỗ, thiếu người lớn...) không bắt ở đây mà để
 * {@code ApiExceptionHandler} đổi thành <b>409</b> kèm câu tiếng Việt.</p>
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartApiController {

    private final CartService cartService;
    private final CartMapper cartMapper;
    private final MessageHelper messages;

    /** Đọc giỏ hàng hiện tại - dùng để cập nhật huy hiệu khi mới tải trang. */
    @GetMapping
    public CartDto view(HttpSession session) {
        return cartMapper.toDto(cartService.getCart(session));
    }

    /**
     * Thêm một dòng vào giỏ.
     *
     * <p>Trả về <b>200</b> chứ không phải 201: thêm trùng đợt khởi hành thì
     * {@code Cart.addOrMerge} cộng dồn vào dòng cũ, không hề sinh ra tài nguyên
     * mới, nên 201 Created sẽ là một lời nói dối trong phần lớn trường hợp.</p>
     */
    @PostMapping("/items")
    public CartDto add(@Valid @RequestBody CartItemRequest request, HttpSession session) {
        CartItem item = request.getDepartureId() != null
                ? cartService.addByDeparture(session, request.getDepartureId(),
                        request.getNumAdults(), request.getNumChildren())
                : cartService.addByTour(session, requireTourId(request),
                        request.getNumAdults(), request.getNumChildren());

        return cartMapper.toDto(cartService.getCart(session))
                .withMessage(messages.get("cart.added", item.getTourName()));
    }

    /**
     * Sửa số khách của một dòng.
     *
     * <p>Tổng số khách về 0 thì dòng bị xoá hẳn - đúng yêu cầu "số lượng bằng 0
     * thì xoá khỏi giỏ" của đề bài, và cũng là lý do phương thức này trả về hai
     * câu thông báo khác nhau.</p>
     */
    @PutMapping("/items/{departureId}")
    public CartDto update(@PathVariable Long departureId,
                          @Valid @RequestBody CartQuantityRequest request,
                          HttpSession session) {
        boolean removed = cartService.updateQuantity(session, departureId,
                request.getNumAdults(), request.getNumChildren());

        return cartMapper.toDto(cartService.getCart(session))
                .withMessage(messages.get(removed ? "cart.removedByZero" : "cart.updated"));
    }

    @DeleteMapping("/items/{departureId}")
    public CartDto remove(@PathVariable Long departureId, HttpSession session) {
        cartService.remove(session, departureId);
        return cartMapper.toDto(cartService.getCart(session))
                .withMessage(messages.get("cart.removed"));
    }

    /** Xoá sạch giỏ - {@code CartService.clear} gỡ hẳn thuộc tính khỏi session. */
    @DeleteMapping
    public CartDto clear(HttpSession session) {
        cartService.clear(session);
        return cartMapper.toDto(cartService.getCart(session))
                .withMessage(messages.get("cart.cleared"));
    }

    // ---------------------------------------------------------------------

    /**
     * Chốt chặn thứ hai cho quy tắc "phải có tourId hoặc departureId".
     *
     * <p>Qua đường HTTP thì {@code @AssertTrue} trong {@link CartItemRequest} đã
     * chặn từ trước và trả về 400. Giữ thêm phép kiểm tra ở đây để nếu có nơi nào
     * gọi thẳng phương thức này (bài kiểm thử chẳng hạn) thì nhận được một lỗi
     * nói rõ nguyên nhân, thay vì một {@code NullPointerException} ở tận tầng
     * truy vấn.</p>
     */
    private Long requireTourId(CartItemRequest request) {
        if (request.getTourId() == null) {
            throw new BusinessRuleException("error.cart.missingTarget");
        }
        return request.getTourId();
    }
}
