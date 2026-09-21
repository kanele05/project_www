package vn.edu.iuh.fit.tourbooking.entity;

/**
 * Loại hành khách - quyết định áp đơn giá người lớn hay đơn giá trẻ em.
 *
 * <p>Số lượng từng loại đã có sẵn ở {@code BookingDetail.numAdults} /
 * {@code numChildren}; enum này cho biết <b>từng người cụ thể</b> thuộc loại nào,
 * và là cơ sở để kiểm tra chéo: đếm hành khách theo loại phải khớp với hai con số
 * kia (quy tắc kiểm tra nằm ở tầng service, không đặt CHECK trong CSDL).</p>
 */
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

    /** Khoá tra cứu trong {@code messages.properties}, ví dụ {@code passenger.type.ADULT}. */
    public String getMessageKey() {
        return "passenger.type." + name();
    }
}
