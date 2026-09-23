package vn.edu.iuh.fit.tourbooking.entity;

// Hình thức thanh toán của một lần thu tiền.
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

    public String getMessageKey() {
        return "payment.method." + name();
    }
}
