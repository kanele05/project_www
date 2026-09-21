package vn.edu.iuh.fit.tourbooking.entity;

/**
 * Trạng thái xử lý một liên hệ gửi từ trang công khai.
 */
public enum ContactStatus {

    /** Mới gửi, chưa ai đọc. */
    NEW("Mới", "danger"),

    /** Nhân viên đã tiếp nhận, đang liên hệ lại với khách. */
    IN_PROGRESS("Đang xử lý", "warning"),

    /** Đã trả lời xong. */
    RESOLVED("Đã xử lý", "success"),

    /** Thư rác - giữ lại để thống kê chứ không xoá. */
    SPAM("Thư rác", "secondary");

    private final String displayName;

    /** Hậu tố lớp CSS của Bootstrap để tô màu nhãn trạng thái. */
    private final String badgeClass;

    ContactStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    /** Khoá tra cứu trong {@code messages.properties}, ví dụ {@code contact.status.NEW}. */
    public String getMessageKey() {
        return "contact.status." + name();
    }
}
