package vn.edu.iuh.fit.tourbooking.entity;

// Trạng thái xử lý một liên hệ gửi từ biểu mẫu công khai.
public enum ContactStatus {

    NEW("Mới", "danger"),

    IN_PROGRESS("Đang xử lý", "warning"),

    RESOLVED("Đã xử lý", "success"),

    SPAM("Thư rác", "secondary");

    private final String displayName;

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

    public String getMessageKey() {
        return "contact.status." + name();
    }
}
