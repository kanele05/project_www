package vn.edu.iuh.fit.tourbooking.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

// Bỏ dấu tiếng Việt và dựng slug URL duy nhất từ một chuỗi bất kỳ.
public final class SlugUtil {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASH = Pattern.compile("(^-+)|(-+$)");

    private SlugUtil() {
    }

    // Bỏ dấu tiếng Việt (xử lý riêng đ/Đ vì Normalizer không tách được) để phục vụ tìm kiếm không dấu.
    public static String removeDiacritics(String input) {
        if (input == null) {
            return null;
        }
        String replaced = input.replace('đ', 'd').replace('Đ', 'D');
        String normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String noAccent = removeDiacritics(input).toLowerCase(Locale.ROOT);
        String dashed = NON_ALNUM.matcher(noAccent).replaceAll("-");
        return EDGE_DASH.matcher(dashed).replaceAll("");
    }

    // Sinh slug và thêm hậu tố -2, -3... tới khi chưa tồn tại.
    public static String toUniqueSlug(String input, java.util.function.Predicate<String> exists) {
        String base = toSlug(input);
        String candidate = base;
        int suffix = 2;
        while (exists.test(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
