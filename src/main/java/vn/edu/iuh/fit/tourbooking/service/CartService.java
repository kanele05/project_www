package vn.edu.iuh.fit.tourbooking.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.config.AppProperties;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.repository.TourDepartureRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.session.Cart;
import vn.edu.iuh.fit.tourbooking.session.CartItem;

/**
 * Quản lý giỏ hàng nằm trong {@code HttpSession}.
 *
 * <p><b>Vì sao thao tác thẳng trên {@code HttpSession} thay vì khai báo một bean
 * {@code @SessionScope}:</b> đề bài yêu cầu chứng minh giỏ hàng được lưu trong
 * Session và "Session được xoá về null" khi thanh toán xong. Với cách làm này,
 * bằng chứng là đúng một dòng {@code session.removeAttribute(CART_ATTRIBUTE)} -
 * chỉ vào là thấy. Dùng bean có phạm vi session thì cơ chế nằm sau lớp proxy của
 * Spring, khó chỉ ra khi vấn đáp.</p>
 *
 * <p>Các phương thức có {@code @Transactional(readOnly = true)} vì chúng đọc CSDL
 * để kiểm tra chỗ trống; bản thân giỏ hàng thì không hề chạm tới CSDL.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    /** Tên thuộc tính trong session. Đặt thành hằng số để không gõ sai ở nơi khác. */
    public static final String CART_ATTRIBUTE = "cart";

    /**
     * Giới hạn mềm cho một dòng, chặn kiểu nghịch nhập 999 khách.
     *
     * <p>Public vì {@code BookingService.updateDetailQuantity} dùng lại đúng
     * hằng số này để kẹp trần tổng khách khi quản trị viên sửa số lượng của một
     * đơn đã đặt - không có lý do gì để hai nơi ấy có hai trần khác nhau.</p>
     */
    public static final int MAX_GUESTS_PER_ITEM = 30;

    private final TourDepartureRepository departureRepository;
    private final TourRepository tourRepository;
    private final AppProperties appProperties;

    /**
     * Lấy giỏ hàng của phiên hiện tại, tạo mới nếu chưa có.
     *
     * <p>Không bao giờ trả về {@code null} nên tầng view không phải kiểm tra.</p>
     */
    public Cart getCart(HttpSession session) {
        Object value = session.getAttribute(CART_ATTRIBUTE);
        if (value instanceof Cart cart) {
            return cart;
        }
        Cart cart = new Cart();
        session.setAttribute(CART_ATTRIBUTE, cart);
        return cart;
    }

    /**
     * Thêm vào giỏ khi khách bấm "Đặt tour" ở <b>trang danh sách</b> - nơi không
     * có chỗ chọn ngày đi.
     *
     * <p>Đề bài yêu cầu chọn được tour ngay từ trang danh sách, nhưng thứ khách
     * thực sự đặt lại là một đợt khởi hành cụ thể. Cách xử lý: tự chọn giúp khách
     * <b>đợt gần nhất còn chỗ</b>; ở trang chi tiết khách vẫn đổi được sang đợt
     * khác. Hai lối đi cùng dẫn về một giỏ hàng.</p>
     */
    @Transactional(readOnly = true)
    public CartItem addByTour(HttpSession session, Long tourId, int adults, int children) {
        if (!tourRepository.existsById(tourId)) {
            throw new BusinessRuleException("error.cart.tourNotFound", tourId);
        }
        validateGuestNumbers(adults, children);
        // minSeats = adults + children: đợt gần nhất mà không đủ chỗ cho đúng số
        // khách khách đang xin thì bỏ qua, tìm tiếp đợt kế - xem Javadoc
        // TourDepartureRepository.findNextBookable.
        TourDeparture departure = departureRepository.findNextBookable(tourId, adults + children,
                        appProperties.booking().cutoffDays())
                .orElseThrow(() -> new BusinessRuleException("error.cart.noDeparture"));
        return addByDeparture(session, departure.getId(), adults, children);
    }

    /** Thêm vào giỏ khi khách chọn đích danh một đợt khởi hành ở trang chi tiết. */
    @Transactional(readOnly = true)
    public CartItem addByDeparture(HttpSession session, Long departureId, int adults, int children) {
        validateGuestNumbers(adults, children);

        TourDeparture departure = departureRepository.findWithTourById(departureId)
                .orElseThrow(() -> new BusinessRuleException("error.cart.departureNotFound", departureId));
        // isBookable() chỉ xét chính đợt khởi hành; tour ngừng bán ("Ngừng bán")
        // vẫn có đợt đang active và còn chỗ, nên phải kiểm riêng tour ở đây - đây
        // là đường thêm vào giỏ từ trang chi tiết (findWithTourById đã nạp sẵn
        // tour bằng @EntityGraph nên đọc tour.isActive() an toàn, không đụng LAZY).
        if (!departure.isBookable(appProperties.booking().cutoffDays()) || !departure.getTour().isActive()) {
            throw new BusinessRuleException("error.cart.notBookable");
        }

        Cart cart = getCart(session);

        // Cộng cả số khách đã có sẵn trong giỏ cho cùng đợt này, nếu không khách
        // có thể bấm "Đặt tour" nhiều lần để vượt quá số chỗ thật.
        CartItem existing = cart.getItem(departureId);
        int alreadyInCart = existing == null ? 0 : existing.getQuantity();
        int wanted = alreadyInCart + adults + children;

        // validateGuestNumbers() ở đầu phương thức chỉ kiểm mỗi lượt thêm mới, KHÔNG
        // biết gì về số khách đã có sẵn trong giỏ - hai lượt thêm 20 + 20 khách (mỗi
        // lượt tự nó hợp lệ, dưới trần) cộng dồn thành một dòng 40 khách, vượt hẳn
        // MAX_GUESTS_PER_ITEM. Phải kiểm lại trần một lần nữa trên tổng SAU khi cộng dồn.
        if (wanted > MAX_GUESTS_PER_ITEM) {
            throw new BusinessRuleException("error.cart.tooManyGuests", MAX_GUESTS_PER_ITEM);
        }
        requireEnoughSeats(departure, wanted);

        cart.addOrMerge(new CartItem(departure, adults, children));
        log.debug("Thêm vào giỏ: đợt {} (+{} người lớn, +{} trẻ em), giỏ còn {} dòng",
                departureId, adults, children, cart.getItemCount());
        return cart.getItem(departureId);
    }

    /**
     * Sửa số lượng khách của một dòng.
     *
     * <p>Đây là chỗ hiện thực yêu cầu "số lượng bằng 0 thì xoá khỏi giỏ" của đề
     * bài: tổng số khách về 0 (hoặc âm do người dùng gõ số âm) thì dòng biến mất
     * hẳn thay vì nằm lại với số lượng 0.</p>
     *
     * @return true nếu dòng đã bị xoá khỏi giỏ
     */
    @Transactional(readOnly = true)
    public boolean updateQuantity(HttpSession session, Long departureId, int adults, int children) {
        Cart cart = getCart(session);
        CartItem item = cart.getItem(departureId);
        if (item == null) {
            throw new BusinessRuleException("error.cart.itemNotFound");
        }

        if (adults + children <= 0) {
            cart.remove(departureId);
            log.debug("Số lượng về 0 - đã xoá đợt {} khỏi giỏ", departureId);
            return true;
        }

        validateGuestNumbers(adults, children);
        TourDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new BusinessRuleException("error.cart.departureNotFound", departureId));
        requireEnoughSeats(departure, adults + children);

        item.setNumAdults(adults);
        item.setNumChildren(children);
        return false;
    }

    public void remove(HttpSession session, Long departureId) {
        getCart(session).remove(departureId);
    }

    /**
     * Xoá sạch giỏ hàng.
     *
     * <p>Gỡ hẳn thuộc tính khỏi session chứ không chỉ gọi {@code cart.clear()}:
     * đề bài yêu cầu "Session được xoá về null" sau khi đặt tour xong, và đây là
     * dòng lệnh thể hiện đúng điều đó. Lần đọc giỏ kế tiếp
     * {@link #getCart(HttpSession)} sẽ tự tạo một giỏ rỗng mới.</p>
     */
    public void clear(HttpSession session) {
        session.removeAttribute(CART_ATTRIBUTE);
        log.debug("Đã xoá giỏ hàng khỏi session");
    }

    // ---------------------------------------------------------------------

    private void validateGuestNumbers(int adults, int children) {
        if (adults < 1) {
            throw new BusinessRuleException("error.cart.needAdult");
        }
        if (children < 0) {
            throw new BusinessRuleException("error.cart.invalidChildren");
        }
        if (adults + children > MAX_GUESTS_PER_ITEM) {
            throw new BusinessRuleException("error.cart.tooManyGuests", MAX_GUESTS_PER_ITEM);
        }
    }

    private void requireEnoughSeats(TourDeparture departure, int wantedSeats) {
        if (!departure.hasEnoughSeats(wantedSeats)) {
            throw new BusinessRuleException("error.cart.notEnoughSeats",
                    departure.getAvailableSeats());
        }
    }
}
