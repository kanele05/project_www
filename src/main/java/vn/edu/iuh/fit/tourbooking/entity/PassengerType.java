package vn.edu.iuh.fit.tourbooking.entity;

// Loại hành khách: người lớn hay trẻ em (quyết định đơn giá áp dụng).
public enum PassengerType {

    ADULT("Người lớn"),
    CHILD("Trẻ em");

    private final String displayName;

    PassengerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMessageKey() {
        return "passenger.type." + name();
    }
}
