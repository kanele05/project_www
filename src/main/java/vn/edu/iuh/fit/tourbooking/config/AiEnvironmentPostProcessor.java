package vn.edu.iuh.fit.tourbooking.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

// Chạy sau khi đọc application.yml: AI tắt thì cũng tắt luôn autoconfig OpenAI của Spring AI, để thiếu hoặc để trống khoá API không làm app sập lúc khởi động.
public class AiEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String SOURCE_NAME = "tourbookingAiDefaults";
    static final String PLACEHOLDER_KEY = "chua-cau-hinh";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = AiProperties.resolveEnabled(
                environment.getProperty("app.ai.enabled"), environment.getProperty("app.ai.api-key"));

        Map<String, Object> overrides = new HashMap<>();
        if (!enabled) {
            overrides.put("spring.ai.model.chat", "none");
            overrides.put("spring.ai.model.embedding", "none");
        }
        if (!StringUtils.hasText(environment.getProperty("spring.ai.openai.api-key"))) {
            overrides.put("spring.ai.openai.api-key", PLACEHOLDER_KEY);
        }
        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, overrides));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
