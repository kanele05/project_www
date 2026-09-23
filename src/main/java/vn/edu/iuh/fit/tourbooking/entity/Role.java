package vn.edu.iuh.fit.tourbooking.entity;

// Vai trò tài khoản: khách hàng hay quản trị viên.
public enum Role {

    CUSTOMER("Khách hàng"),

    ADMIN("Quản trị viên");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMessageKey() {
        return "user.role." + name();
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
