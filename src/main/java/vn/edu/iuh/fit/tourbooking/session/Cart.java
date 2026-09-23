package vn.edu.iuh.fit.tourbooking.session;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

// Giỏ hàng POJO lưu trong HttpSession (không phải entity) - đề bài yêu cầu giỏ nằm trong Session.
public class Cart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<Long, CartItem> items = new LinkedHashMap<>();

    public Collection<CartItem> getItems() {
        return items.values();
    }

    public CartItem getItem(Long departureId) {
        return items.get(departureId);
    }

    public boolean contains(Long departureId) {
        return items.containsKey(departureId);
    }

    public void addOrMerge(CartItem item) {
        CartItem existing = items.get(item.getDepartureId());
        if (existing == null) {
            items.put(item.getDepartureId(), item);
        } else {
            existing.merge(item.getNumAdults(), item.getNumChildren());
        }
    }

    public void remove(Long departureId) {
        items.remove(departureId);
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getItemCount() {
        return items.size();
    }

    public int getTotalQuantity() {
        return items.values().stream().mapToInt(CartItem::getQuantity).sum();
    }

    public BigDecimal getTotalAmount() {
        return items.values().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
