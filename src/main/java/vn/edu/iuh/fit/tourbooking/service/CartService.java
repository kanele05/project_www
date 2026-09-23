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

@Service
@RequiredArgsConstructor
@Slf4j
// Quản lý giỏ hàng nằm trong HttpSession (không phải entity, không chạm CSDL khi đọc/ghi giỏ).
public class CartService {

    public static final String CART_ATTRIBUTE = "cart";

    public static final int MAX_GUESTS_PER_ITEM = 30;

    private final TourDepartureRepository departureRepository;
    private final TourRepository tourRepository;
    private final AppProperties appProperties;

    // Lấy giỏ hàng từ session, tạo mới nếu chưa có.
    public Cart getCart(HttpSession session) {
        Object value = session.getAttribute(CART_ATTRIBUTE);
        if (value instanceof Cart cart) {
            return cart;
        }
        Cart cart = new Cart();
        session.setAttribute(CART_ATTRIBUTE, cart);
        return cart;
    }

    // Thêm vào giỏ theo tour: tự chọn đợt khởi hành gần nhất còn đủ chỗ rồi giao lại cho addByDeparture.
    @Transactional(readOnly = true)
    public CartItem addByTour(HttpSession session, Long tourId, int adults, int children) {
        if (!tourRepository.existsById(tourId)) {
            throw new BusinessRuleException("error.cart.tourNotFound", tourId);
        }
        validateGuestNumbers(adults, children);

        TourDeparture departure = departureRepository.findNextBookable(tourId, adults + children,
                        appProperties.booking().cutoffDays())
                .orElseThrow(() -> new BusinessRuleException("error.cart.noDeparture"));
        return addByDeparture(session, departure.getId(), adults, children);
    }

    // Thêm vào giỏ theo đợt cụ thể: kiểm còn bán, cộng dồn nếu đã có dòng cùng đợt, chặn vượt trần khách.
    @Transactional(readOnly = true)
    public CartItem addByDeparture(HttpSession session, Long departureId, int adults, int children) {
        validateGuestNumbers(adults, children);

        TourDeparture departure = departureRepository.findWithTourById(departureId)
                .orElseThrow(() -> new BusinessRuleException("error.cart.departureNotFound", departureId));

        if (!departure.isBookable(appProperties.booking().cutoffDays()) || !departure.getTour().isActive()) {
            throw new BusinessRuleException("error.cart.notBookable");
        }

        Cart cart = getCart(session);

        CartItem existing = cart.getItem(departureId);
        int alreadyInCart = existing == null ? 0 : existing.getQuantity();
        int wanted = alreadyInCart + adults + children;

        if (wanted > MAX_GUESTS_PER_ITEM) {
            throw new BusinessRuleException("error.cart.tooManyGuests", MAX_GUESTS_PER_ITEM);
        }
        requireEnoughSeats(departure, wanted);

        cart.addOrMerge(new CartItem(departure, adults, children));
        log.debug("Thêm vào giỏ: đợt {} (+{} người lớn, +{} trẻ em), giỏ còn {} dòng",
                departureId, adults, children, cart.getItemCount());
        return cart.getItem(departureId);
    }

    // Sửa số khách của một dòng trong giỏ; về 0 khách thì xoá luôn dòng đó. Trả về true nếu đã xoá.
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

    public void clear(HttpSession session) {
        session.removeAttribute(CART_ATTRIBUTE);
        log.debug("Đã xoá giỏ hàng khỏi session");
    }

    // Kiểm số khách hợp lệ: ít nhất 1 người lớn, trẻ em không âm, tổng không vượt trần.
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
