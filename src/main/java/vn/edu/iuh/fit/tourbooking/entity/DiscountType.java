package vn.edu.iuh.fit.tourbooking.entity;

/**
 * Cách tính giảm giá của một chương trình khuyến mãi.
 *
 * <p>{@link #PERCENT} luôn đi kèm trần giảm ({@code Promotion.maxDiscount}) - nếu
 * không, một mã "giảm 20%" áp vào đơn 100 triệu sẽ thành 20 triệu.</p>
 */
public enum DiscountType {

    /** Giảm theo phần trăm giá trị đơn. */
    PERCENT("Giảm theo phần trăm"),

    /** Giảm một số tiền cố định. */
    AMOUNT("Giảm số tiền cố định");

    private final String displayName;

    DiscountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Khoá tra cứu trong {@code messages.properties}, ví dụ {@code promotion.type.PERCENT}. */
    public String getMessageKey() {
        return "promotion.type." + name();
    }
}
