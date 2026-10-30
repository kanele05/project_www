package vn.edu.iuh.fit.tourbooking.ai;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.util.StringUtils;

import java.util.List;

// Dựng Client của Google GenAI SDK cho chat: khoá API, địa chỉ máy chủ (mặc định của SDK nếu để trống), thời gian chờ, thử lại 429/503.
// Vì sao tự dựng: autoconfig của Spring AI 1.1.8 không có thuộc tính base-url/timeout/retry, và RetryTemplate của Spring AI
// chỉ thử lại TransientAiException - lỗi 429 từ SDK (ApiException) sẽ không được thử lại nếu không cấu hình ở đây.
public final class GenAiClientFactory {

    // Khoá giả để app vẫn khởi động được khi bật AI mà quên khoá (gọi thật sẽ trả 400/401 và bị bắt ở AiChatService).
    static final String PLACEHOLDER_KEY = "chua-cau-hinh";

    private GenAiClientFactory() {
    }

    public static Client create(String apiKey, String baseUrl, int timeoutMs, double retryDelaySeconds) {
        HttpOptions.Builder http = HttpOptions.builder()
                .timeout(timeoutMs)
                .retryOptions(HttpRetryOptions.builder()
                        .attempts(3)
                        .initialDelay(retryDelaySeconds)
                        .maxDelay(Math.max(retryDelaySeconds, 15.0))
                        .expBase(2.0)
                        .httpStatusCodes(List.of(429, 503)));
        if (StringUtils.hasText(baseUrl)) {
            http.baseUrl(baseUrl.strip());
        }
        return Client.builder()
                .apiKey(StringUtils.hasText(apiKey) ? apiKey : PLACEHOLDER_KEY)
                .httpOptions(http.build())
                .build();
    }
}
