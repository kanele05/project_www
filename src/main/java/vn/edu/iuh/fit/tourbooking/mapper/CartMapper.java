package vn.edu.iuh.fit.tourbooking.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.tourbooking.dto.view.CartDto;
import vn.edu.iuh.fit.tourbooking.dto.view.CartItemDto;
import vn.edu.iuh.fit.tourbooking.session.Cart;
import vn.edu.iuh.fit.tourbooking.session.CartItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
// Chuyển Cart/CartItem (session) sang DTO hiển thị, ghép URL ảnh và định dạng ngày.
public class CartMapper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ViewUrls urls;

    public CartDto toDto(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream().map(this::toItemDto).toList();

        return new CartDto(
                items,
                cart.getItemCount(),
                cart.getTotalQuantity(),
                cart.getTotalAmount(),
                null);
    }

    public CartItemDto toItemDto(CartItem item) {
        return new CartItemDto(
                item.getDepartureId(),
                item.getTourId(),
                item.getTourName(),
                item.getTourSlug(),
                urls.image(item.getThumbnail()),
                urls.tourDetail(item.getTourId(), item.getTourSlug()),
                item.getDepartureDate(),
                format(item.getDepartureDate()),
                item.getNumAdults(),
                item.getNumChildren(),
                item.getQuantity(),
                item.getPriceAdult(),
                item.getPriceChild(),
                item.getSubtotal());
    }

    private String format(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMAT);
    }
}
