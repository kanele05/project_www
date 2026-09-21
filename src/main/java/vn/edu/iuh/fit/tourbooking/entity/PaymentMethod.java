package vn.edu.iuh.fit.tourbooking.entity;

/**
 * Hình thức thanh toán của một lần trả tiền.
 *
 * <p>Ba giá trị này khớp với ba lựa chọn đang có ở trang thanh toán
 * ({@code checkout.payment.bank|cash|momo}). Cột {@code bookings.payment_method}
 * hiện vẫn lưu chuỗi hiển thị do người dùng chọn - bảng {@code payments} mới là
 * nơi lưu hình thức dưới dạng mã, để thống kê theo hình thức không phụ thuộc vào
 * ngôn ngữ đang xem.</p>
 */
public enum PaymentMethod {

    BANK_TRANSFER("Chuyển khoản ngân hàng"),
    CASH("Tiền mặt tại văn phòng"),
    MOMO("Ví MoMo");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Khoá tra cứu trong {@code messages.properties}, ví dụ {@code payment.method.MOMO}. */
    public String getMessageKey() {
        return "payment.method." + name();
    }
}
