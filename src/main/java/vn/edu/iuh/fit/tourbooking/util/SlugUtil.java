package vn.edu.iuh.fit.tourbooking.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chuyển tiêu đề tiếng Việt thành chuỗi thân thiện với URL.
 *
 * <p>{@code "Đà Nẵng - Hội An 4N3Đ"} &rarr; {@code "da-nang-hoi-an-4n3d"}.</p>
 *
 * <p>Cùng thuật toán bỏ dấu này còn được dùng cho tìm kiếm không dấu, nên tách
 * riêng {@link #removeDiacritics(String)} để nơi khác gọi lại được.</p>
 */
public final class SlugUtil {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASH = Pattern.compile("(^-+)|(-+$)");

    private SlugUtil() {
    }

    /**
     * Bỏ dấu tiếng Việt.
     *
     * <p>Riêng chữ {@code đ/Đ} phải thay tay: {@code Normalizer} chỉ tách được
     * dấu phụ khỏi nguyên âm, còn {@code đ} là một ký tự Latin độc lập chứ không
     * phải {@code d} cộng dấu, nên tách kiểu gì cũng không ra {@code d}.</p>
     */
    public static String removeDiacritics(String input) {
        if (input == null) {
            return null;
        }
        String replaced = input.replace('đ', 'd').replace('Đ', 'D');
        String normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }

    /**
     * Sinh slug: bỏ dấu, hạ chữ thường, thay mọi ký tự không phải chữ/số bằng
     * dấu gạch ngang rồi cắt gạch thừa ở hai đầu.
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String noAccent = removeDiacritics(input).toLowerCase(Locale.ROOT);
        String dashed = NON_ALNUM.matcher(noAccent).replaceAll("-");
        return EDGE_DASH.matcher(dashed).replaceAll("");
    }

    /**
     * Slug đảm bảo không trùng: nếu {@code exists} báo đã có thì nối thêm
     * {@code -2}, {@code -3}... cho tới khi tìm được chuỗi còn trống.
     *
     * @param input  tiêu đề gốc
     * @param exists hàm kiểm tra slug đã tồn tại trong CSDL hay chưa
     */
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
