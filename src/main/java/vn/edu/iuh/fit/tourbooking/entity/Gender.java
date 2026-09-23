package vn.edu.iuh.fit.tourbooking.entity;

// Giới tính của hành khách.
public enum Gender {

    MALE("Nam"),
    FEMALE("Nữ"),
    OTHER("Khác");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMessageKey() {
        return "gender." + name();
    }
}
