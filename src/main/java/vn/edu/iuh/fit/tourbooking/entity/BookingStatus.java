package vn.edu.iuh.fit.tourbooking.entity;

// Trạng thái đơn hàng và máy trạng thái chuyển đổi hợp lệ giữa chúng.
public enum BookingStatus {

    PENDING("Chờ xác nhận", "warning"),

    CONFIRMED("Đã xác nhận", "info"),

    COMPLETED("Hoàn thành", "success"),

    CANCELLED("Đã huỷ", "danger");

    private final String displayName;

    private final String badgeClass;

    BookingStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMessageKey() {
        return "booking.status." + name();
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    // Có được phép chuyển từ trạng thái hiện tại sang "target" hay không.
    public boolean canTransitionTo(BookingStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
