package vn.edu.iuh.fit.tourbooking.entity;

// Cách tính giảm giá của một mã khuyến mãi: theo phần trăm hay số tiền cố định.
public enum DiscountType {

    PERCENT("Giảm theo phần trăm"),

    AMOUNT("Giảm số tiền cố định");

    private final String displayName;

    DiscountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMessageKey() {
        return "promotion.type." + name();
    }
}
