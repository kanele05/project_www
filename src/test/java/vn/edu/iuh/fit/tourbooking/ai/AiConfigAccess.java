package vn.edu.iuh.fit.tourbooking.ai;

import java.lang.reflect.Field;

// Cho kiểm thử đọc system prompt gói-riêng trong AiConfig mà không phải mở rộng phạm vi truy cập của mã chính.
final class AiConfigAccess {

    private AiConfigAccess() {
    }

    static String systemPrompt() {
        try {
            Field f = Class.forName("vn.edu.iuh.fit.tourbooking.config.AiConfig").getDeclaredField("SYSTEM_PROMPT");
            f.setAccessible(true);
            return (String) f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
