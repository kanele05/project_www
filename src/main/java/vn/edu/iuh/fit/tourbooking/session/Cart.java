package vn.edu.iuh.fit.tourbooking.session;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Giỏ hàng của một phiên làm việc.
 *
 * <p>Được lưu trong {@code HttpSession} dưới thuộc tính tên {@code "cart"}
 * (xem {@code CartService}). Mỗi trình duyệt có một phiên riêng nên giỏ hàng
 * của hai khách không lẫn vào nhau - mở thêm một cửa sổ ẩn danh sẽ thấy giỏ rỗng.</p>
 *
 * <p>Dùng {@link LinkedHashMap} chứ không dùng {@code List}: khoá là mã đợt khởi
 * hành nên việc gộp dòng khi khách đặt trùng đợt chỉ là một phép tra khoá, đồng
 * thời vẫn giữ nguyên thứ tự khách thêm vào giỏ (nếu dùng {@code HashMap} thì
 * thứ tự các dòng sẽ nhảy loạn mỗi lần tải lại trang).</p>
 */
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

    /**
     * Thêm một dòng vào giỏ. Nếu đợt khởi hành đó đã có sẵn thì <b>cộng dồn</b>
     * số khách vào dòng cũ thay vì tạo thêm dòng thứ hai.
     */
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

    /** Số dòng trong giỏ - hiển thị trên huy hiệu ở thanh điều hướng. */
    public int getItemCount() {
        return items.size();
    }

    /** Tổng số khách của cả giỏ. */
    public int getTotalQuantity() {
        return items.values().stream().mapToInt(CartItem::getQuantity).sum();
    }

    public BigDecimal getTotalAmount() {
        return items.values().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
