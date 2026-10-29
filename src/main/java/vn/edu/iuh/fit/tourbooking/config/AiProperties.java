package vn.edu.iuh.fit.tourbooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

// Cấu hình tính năng AI (tiền tố "app.ai."): công tắc, nhà cung cấp (mặc định Google Gemini qua endpoint tương thích OpenAI; đổi base-url để dùng OpenAI/Ollama), kho vector và hạn mức.
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(Boolean enabled,
                           String apiKey,
                           @DefaultValue("https://generativelanguage.googleapis.com") String baseUrl,
                           @DefaultValue("gemini-2.5-flash") String chatModel,
                           @DefaultValue("gemini-embedding-001") String embeddingModel,
                           Integer embeddingDimensions,
                           @DefaultValue("700") long embeddingMinIntervalMs,
                           @DefaultValue("data/vector-store.json") String vectorStorePath,
                           @DefaultValue("0.5") double similarityThreshold,
                           @DefaultValue("30") int searchTopK,
                           @DefaultValue("4") int ragTopK,
                           @DefaultValue("20") int memoryMessages,
                           @DefaultValue("10") int chatPerMinute,
                           @DefaultValue("500") int maxMessageLength) {

    // Công tắc tường minh (AI_ENABLED) thắng; để trống thì tự bật khi có khoá API.
    public static boolean resolveEnabled(String flag, String apiKey) {
        if (StringUtils.hasText(flag)) {
            return Boolean.parseBoolean(flag.trim());
        }
        return StringUtils.hasText(apiKey);
    }

    public boolean active() {
        return resolveEnabled(enabled == null ? null : enabled.toString(), apiKey);
    }
}
