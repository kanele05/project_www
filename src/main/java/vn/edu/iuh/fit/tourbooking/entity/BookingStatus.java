package vn.edu.iuh.fit.tourbooking.entity;

/**
 * Trạng thái của một đơn đặt tour.
 *
 * <p>Vòng đời bình thường: {@code PENDING -> CONFIRMED -> COMPLETED}.
 * Ở bất kỳ bước nào trước {@code COMPLETED} đều có thể chuyển sang
 * {@code CANCELLED}; khi huỷ thì số chỗ đã giữ được trả lại cho chuyến khởi hành.</p>
 */
public enum BookingStatus {

    /** Vừa đặt, chờ quản trị viên xác nhận. */
    PENDING("Chờ xác nhận", "warning"),

    /** Đã xác nhận, khách chuẩn bị đi. */
    CONFIRMED("Đã xác nhận", "info"),

    /** Tour đã kết thúc. */
    COMPLETED("Hoàn thành", "success"),

    /** Đã huỷ - số chỗ được hoàn lại cho chuyến khởi hành. */
    CANCELLED("Đã huỷ", "danger");

    private final String displayName;

    /** Hậu tố lớp CSS của Bootstrap để tô màu nhãn trạng thái trên giao diện. */
    private final String badgeClass;

    BookingStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    /**
     * Tên tiếng Việt cố định, dùng ở những chỗ không có ngữ cảnh ngôn ngữ của
     * người dùng - ví dụ dòng ghi log.
     *
     * <p>Trên giao diện thì dùng {@link #getMessageKey()}, xem lý do ở đó.</p>
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Khoá tra cứu trong {@code messages.properties}, ví dụ
     * {@code booking.status.PENDING}.
     *
     * <p>Sinh từ {@code name()} thay vì khai báo tay cho từng hằng: thêm một
     * trạng thái mới thì không thể quên khai báo khoá, cùng lắm là thiếu dòng
     * dịch và Thymeleaf hiện ra {@code ??booking.status.X??} - lỗi nhìn thấy
     * ngay, hơn hẳn một chuỗi tiếng Việt lọt vào bản tiếng Anh mà không ai để ý.</p>
     */
    public String getMessageKey() {
        return "booking.status." + name();
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    /** Đơn đã kết thúc vòng đời thì không cho sửa số lượng khách nữa. */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * Máy trạng thái đơn hàng (SPEC_CHUNG.md mục 12.1):
     * <pre>
     * PENDING   -&gt; CONFIRMED | CANCELLED
     * CONFIRMED -&gt; COMPLETED | CANCELLED
     * COMPLETED, CANCELLED: trạng thái CUỐI, không chuyển tiếp đi đâu nữa -
     *   cố ý KHÔNG có nhánh "khôi phục đơn đã huỷ".
     * </pre>
     *
     * <p>Đây chỉ là phép kiểm <b>cấu trúc</b> (đúng hình trên sơ đồ). Điều kiện
     * phụ để vào {@code COMPLETED} ("ngày khởi hành đã tới" + "đã có khoản
     * ĐÃ THANH TOÁN") cần dữ liệu ngoài enum này, nên được kiểm riêng ở
     * {@code BookingService.updateStatus}.</p>
     */
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
