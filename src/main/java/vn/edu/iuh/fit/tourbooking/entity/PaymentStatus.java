package vn.edu.iuh.fit.tourbooking.entity;

/**
 * Trạng thái của một lần thanh toán.
 *
 * <p>Tách khỏi {@link BookingStatus} có chủ đích: một đơn có thể đã xác nhận mà
 * chưa thu đủ tiền (đặt cọc), hoặc đã huỷ nhưng còn phải hoàn tiền. Gộp hai khái
 * niệm vào một cột là chỗ mà các hệ thống bán tour hay sai.</p>
 */
public enum PaymentStatus {

    /** Đã ghi nhận yêu cầu, chưa nhận được tiền. */
    PENDING("Chờ thanh toán", "warning"),

    /** Đã nhận đủ số tiền của lần thanh toán này. */
    PAID("Đã thanh toán", "success"),

    /** Giao dịch thất bại (chuyển khoản sai nội dung, thẻ bị từ chối...). */
    FAILED("Thất bại", "danger"),

    /** Đã hoàn tiền cho khách sau khi huỷ đơn. */
    REFUNDED("Đã hoàn tiền", "secondary");

    private final String displayName;

    /** Hậu tố lớp CSS của Bootstrap để tô màu nhãn trạng thái. */
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

    /** Khoá tra cứu trong {@code messages.properties}, ví dụ {@code payment.status.PAID}. */
    public String getMessageKey() {
        return "payment.status." + name();
    }

    /** Chỉ lần thanh toán đã thu được tiền mới được tính vào doanh thu. */
    public boolean isSettled() {
        return this == PAID;
    }
}
