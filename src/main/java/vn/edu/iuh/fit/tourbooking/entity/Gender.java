package vn.edu.iuh.fit.tourbooking.entity;

/**
 * Giới tính của một hành khách trong đơn.
 *
 * <p>Để dạng enum thay vì chuỗi tự do vì danh sách hành khách là thứ được in ra
 * cho đối tác (hãng bay, khách sạn) - dữ liệu nhập tay kiểu "nam"/"Nam"/"M" sẽ
 * làm hỏng khâu xuất danh sách.</p>
 */
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

    /** Khoá tra cứu trong {@code messages.properties}, ví dụ {@code gender.MALE}. */
    public String getMessageKey() {
        return "gender." + name();
    }
}
