package vn.edu.iuh.fit.tourbooking.ai;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Rút khoảng giá từ câu tìm kiếm tự nhiên ("dưới 5 triệu", "từ 2 đến 4 triệu", "tối đa 3tr") để dùng làm bộ lọc metadata.
public final class PriceHintParser {

    public record Hint(Long minPrice, Long maxPrice, String cleanedQuery) {
        public boolean hasPrice() {
            return minPrice != null || maxPrice != null;
        }
    }

    private static final String NUMBER = "(\\d{1,3}(?:\\.\\d{3})+|\\d+(?:[.,]\\d+)?)";
    private static final String UNIT = "(?:\\s*(triệu|trieu|tr|nghìn|nghin|ngàn|ngan|k|đồng|dong|đ|vnd))?";
    private static final String AMOUNT = NUMBER + UNIT;

    private static final Pattern RANGE = Pattern.compile(
            "(?:từ|tu)\\s*" + AMOUNT + "\\s*(?:đến|den|tới|toi|-)\\s*" + AMOUNT + "(?![\\p{L}\\d])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern MAX = Pattern.compile(
            "(?:dưới|duoi|không quá|khong qua|tối đa|toi da|nhỏ hơn|nho hon|rẻ hơn|re hon|<=|<)\\s*" + AMOUNT
                    + "(?![\\p{L}\\d])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern MIN = Pattern.compile(
            "(?:trên|tren|từ|tu|ít nhất|it nhat|lớn hơn|lon hon|>=|>)\\s*" + AMOUNT + "(?![\\p{L}\\d])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private PriceHintParser() {
    }

    // Phân tích câu truy vấn; trả về khoảng giá (nếu có) và câu truy vấn đã bỏ cụm từ chỉ giá.
    public static Hint parse(String query) {
        if (query == null || query.isBlank()) {
            return new Hint(null, null, "");
        }
        String text = query.trim();

        Matcher range = RANGE.matcher(text);
        if (range.find()) {
            String unitA = range.group(2) != null ? range.group(2) : range.group(4);
            String unitB = range.group(4) != null ? range.group(4) : range.group(2);
            Long a = toVnd(range.group(1), unitA);
            Long b = toVnd(range.group(3), unitB);
            if (a != null && b != null) {
                return new Hint(Math.min(a, b), Math.max(a, b), clean(text.replace(range.group(), " ")));
            }
        }

        Long max = null;
        Long min = null;
        Matcher m = MAX.matcher(text);
        if (m.find()) {
            max = toVnd(m.group(1), m.group(2));
            if (max != null) {
                text = text.replace(m.group(), " ");
            }
        }
        m = MIN.matcher(text);
        if (m.find()) {
            min = toVnd(m.group(1), m.group(2));
            if (min != null) {
                text = text.replace(m.group(), " ");
            }
        }
        return new Hint(min, max, clean(text));
    }

    // Đổi "5", "triệu" -> 5_000_000; không có đơn vị mà số nhỏ (< 1000) thì hiểu là triệu đồng.
    static Long toVnd(String number, String unit) {
        if (number == null) {
            return null;
        }
        String normalized = number.contains(".") && number.matches("\\d{1,3}(\\.\\d{3})+")
                ? number.replace(".", "")
                : number.replace(',', '.');
        double value;
        try {
            value = Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
        String u = unit == null ? "" : unit.toLowerCase(Locale.ROOT);
        double multiplier = switch (u) {
            case "triệu", "trieu", "tr" -> 1_000_000;
            case "nghìn", "nghin", "ngàn", "ngan", "k" -> 1_000;
            case "đồng", "dong", "đ", "vnd" -> 1;
            default -> value < 1000 ? 1_000_000 : 1;
        };
        long vnd = Math.round(value * multiplier);
        return vnd > 0 ? vnd : null;
    }

    private static String clean(String text) {
        return text.replaceAll("\\s+", " ").replaceAll("^[\\s,;.]+|[\\s,;.]+$", "");
    }
}
