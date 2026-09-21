package vn.edu.iuh.fit.tourbooking.entity;

/**
 * Vai trò của người dùng.
 *
 * <p>Cố tình để là <b>enum</b> chứ không tách thành bảng {@code roles} riêng:
 * hệ thống chỉ có đúng hai vai trò và không có nhu cầu thêm vai trò lúc chạy,
 * nên một bảng tra cứu 2 dòng chỉ làm phức tạp thêm câu JOIN.</p>
 *
 * <p>Spring Security quy ước tên quyền phải có tiền tố {@code ROLE_} khi dùng
 * {@code hasRole("ADMIN")}, vì vậy có sẵn {@link #authority()} để tránh viết
 * chuỗi tay ở nhiều nơi.</p>
 */
public enum Role {

    /** Khách hàng: xem tour, đặt tour, quản lý đơn của chính mình. */
    CUSTOMER("Khách hàng"),

    /** Quản trị viên: toàn quyền trên khu vực {@code /admin}. */
    ADMIN("Quản trị viên");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Khoá tra cứu trong {@code messages.properties}, ví dụ {@code user.role.ADMIN}. */
    public String getMessageKey() {
        return "user.role." + name();
    }

    /** Chuỗi quyền theo quy ước của Spring Security, ví dụ {@code ROLE_ADMIN}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
