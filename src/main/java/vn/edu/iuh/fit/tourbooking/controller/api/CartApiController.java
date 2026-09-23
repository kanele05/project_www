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

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
// REST AJAX cho giỏ hàng: thêm/sửa/xoá dòng, luôn thao tác trên giỏ nằm trong Session.
public class CartApiController {

    private final CartService cartService;
    private final CartMapper cartMapper;
    private final MessageHelper messages;

    @GetMapping
    public CartDto view(HttpSession session) {
        return cartMapper.toDto(cartService.getCart(session));
    }

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

    @DeleteMapping
    public CartDto clear(HttpSession session) {
        cartService.clear(session);
        return cartMapper.toDto(cartService.getCart(session))
                .withMessage(messages.get("cart.cleared"));
    }

    private Long requireTourId(CartItemRequest request) {
        if (request.getTourId() == null) {
            throw new BusinessRuleException("error.cart.missingTarget");
        }
        return request.getTourId();
    }
}
