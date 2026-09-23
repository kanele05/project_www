package vn.edu.iuh.fit.tourbooking.entity;

// Trạng thái một lần thanh toán.
public enum PaymentStatus {

    PENDING("Chờ thanh toán", "warning"),

    PAID("Đã thanh toán", "success"),

    FAILED("Thất bại", "danger"),

    REFUNDED("Đã hoàn tiền", "secondary");

    private final String displayName;

    private final String badgeClass;

    PaymentStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getMessageKey() {
        return "payment.status." + name();
    }

    public boolean isSettled() {
        return this == PAID;
    }
}
