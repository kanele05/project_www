package vn.edu.iuh.fit.tourbooking.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Sinh mã đơn đặt tour, dạng {@code TB20260807A1B2C3}.
 *
 * <p>Mã này là thứ khách hàng nhìn thấy và là thứ đi trong URL
 * {@code /account/bookings/{code}}. Cố ý <b>không</b> dùng khoá chính tự tăng
 * làm mã: id tuần tự cho phép người ngoài đoán đơn của người khác chỉ bằng cách
 * sửa số trên thanh địa chỉ, đồng thời để lộ số đơn hệ thống đã bán.</p>
 *
 * <p>Phần ngẫu nhiên lấy từ {@link SecureRandom} và bỏ các ký tự dễ đọc nhầm
 * ({@code I, O, 0, 1}) để nhân viên đọc mã qua điện thoại không bị sai.</p>
 */
public final class CodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int RANDOM_LENGTH = 6;
    private static final DateTimeFormatter DATE_PART = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeGenerator() {
    }

    /** Sinh một mã đơn mới. Nơi gọi phải kiểm tra trùng và sinh lại nếu cần. */
    public static String bookingCode() {
        StringBuilder sb = new StringBuilder("TB").append(LocalDate.now().format(DATE_PART));
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Sinh mã đơn chắc chắn chưa tồn tại trong CSDL.
     *
     * @param exists hàm kiểm tra mã đã có hay chưa
     */
    public static String uniqueBookingCode(java.util.function.Predicate<String> exists) {
        String code = bookingCode();
        while (exists.test(code)) {
            code = bookingCode();
        }
        return code;
    }
}
