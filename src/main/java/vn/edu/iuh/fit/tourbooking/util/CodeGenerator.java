package vn.edu.iuh.fit.tourbooking.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// Sinh mã đơn hàng dạng TB + ngày + chuỗi ngẫu nhiên, đảm bảo không trùng.
public final class CodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int RANDOM_LENGTH = 6;
    private static final DateTimeFormatter DATE_PART = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeGenerator() {
    }

    public static String bookingCode() {
        StringBuilder sb = new StringBuilder("TB").append(LocalDate.now().format(DATE_PART));
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    // Sinh mã và thử lại tới khi chưa tồn tại (theo predicate exists truyền vào).
    public static String uniqueBookingCode(java.util.function.Predicate<String> exists) {
        String code = bookingCode();
        while (exists.test(code)) {
            code = bookingCode();
        }
        return code;
    }
}
