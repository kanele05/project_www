package vn.edu.iuh.fit.tourbooking.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import com.google.genai.Client;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatProperties;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.chat.memory.autoconfigure.ChatMemoryAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingProperties;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.List;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import vn.edu.iuh.fit.tourbooking.ai.TourTools;
import vn.edu.iuh.fit.tourbooking.ai.TourVectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// Dựng thật chuỗi cấu hình Spring AI (đọc application.properties thật) để chứng minh: không khoá thì không có bean AI, có khoá thì nối đúng Gemini.
class AiWiringTest {

    @Configuration
    @EnableConfigurationProperties(AiProperties.class)
    @Import(AiConfig.class)
    @ImportAutoConfiguration({RestClientAutoConfiguration.class, WebClientAutoConfiguration.class,
            SpringAiRetryAutoConfiguration.class, ToolCallingAutoConfiguration.class,
            GoogleGenAiChatAutoConfiguration.class, OpenAiChatAutoConfiguration.class, OpenAiEmbeddingAutoConfiguration.class,
            ChatClientAutoConfiguration.class, ChatMemoryAutoConfiguration.class})
    static class Support {
        @Bean
        TourTools tourTools() {
            return mock(TourTools.class);
        }
    }

    // Chạy bằng SpringApplication thật (có EnvironmentPostProcessor và application.properties thật), không dùng web/DB.
    private ConfigurableApplicationContext start(String... properties) {
        List<String> all = new java.util.ArrayList<>(List.of("spring.main.web-application-type=none",
                "spring.main.banner-mode=off", "logging.level.root=WARN"));
        all.addAll(List.of(properties));
        return new SpringApplicationBuilder(Support.class).properties(all.toArray(String[]::new)).run();
    }

    @Test
    void without_a_key_the_app_starts_and_no_ai_beans_are_created() {
        try (var ctx = start("GEMINI_API_KEY=")) {
            assertThat(ctx.getBeansOfType(TourVectorStore.class)).isEmpty();
            assertThat(ctx.getBeansOfType(ChatClient.class)).isEmpty();
            assertThat(ctx.getBeansOfType(OpenAiChatModel.class)).isEmpty();
            assertThat(ctx.getBeansOfType(GoogleGenAiChatModel.class)).isEmpty();
            assertThat(ctx.getBeansOfType(Client.class)).isEmpty();
            assertThat(ctx.getBean(AiProperties.class).active()).isFalse();
        }
    }

    @Test
    void a_blank_environment_variable_does_not_crash_startup_either() {
        try (var ctx = start("GEMINI_API_KEY=", "AI_ENABLED=")) {
            assertThat(ctx.getBeansOfType(ChatClient.class)).isEmpty();
        }
    }

    @Test
    void an_explicit_off_switch_beats_a_present_key() {
        try (var ctx = start("GEMINI_API_KEY=AIza-test", "AI_ENABLED=false")) {
            assertThat(ctx.getBeansOfType(TourVectorStore.class)).isEmpty();
            assertThat(ctx.getBeansOfType(ChatClient.class)).isEmpty();
        }
    }

    @Test
    void with_a_key_chat_goes_through_the_native_gemini_sdk_and_embeddings_through_the_openai_compat_endpoint() {
        try (var ctx = start("GEMINI_API_KEY=AIza-test")) {
            assertThat(ctx.getBeansOfType(TourVectorStore.class)).hasSize(1);
            assertThat(ctx.containsBean("tourAssistant")).isTrue();
            assertThat(ctx.containsBean("tourAssistantRagOnly")).isTrue();

            // Chat: Google GenAI SDK (giữ thought signature), KHÔNG phải mô hình chat OpenAI-compat.
            assertThat(ctx.getBeansOfType(GoogleGenAiChatModel.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(OpenAiChatModel.class)).isEmpty();
            assertThat(ctx.getBeansOfType(Client.class)).hasSize(1);
            GoogleGenAiChatProperties chat = ctx.getBean(GoogleGenAiChatProperties.class);
            assertThat(chat.getOptions().getModel()).isEqualTo("gemini-2.5-flash");
            assertThat(chat.getOptions().getTemperature()).isEqualTo(0.3);
            // KHÔNG bật includeThoughts: Spring AI 1.1.8 không lọc phần "thought" nên bật sẽ làm lọt tóm tắt suy nghĩ vào câu trả lời.
            assertThat(chat.getOptions().getIncludeThoughts()).isNull();

            // Nhúng vector: vẫn qua starter OpenAI trỏ tới endpoint tương thích của Gemini, cùng mô hình/số chiều như chỉ mục đã lưu.
            assertThat(ctx.getBeansOfType(OpenAiEmbeddingModel.class)).hasSize(1);
            OpenAiConnectionProperties connection = ctx.getBean(OpenAiConnectionProperties.class);
            assertThat(connection.getBaseUrl()).isEqualTo("https://generativelanguage.googleapis.com");
            assertThat(connection.getApiKey()).isEqualTo("AIza-test");
            OpenAiEmbeddingProperties embedding = ctx.getBean(OpenAiEmbeddingProperties.class);
            assertThat(embedding.getEmbeddingsPath()).isEqualTo("/v1beta/openai/embeddings");
            assertThat(embedding.getOptions().getModel()).isEqualTo("gemini-embedding-001");
            assertThat(embedding.getOptions().getDimensions()).isNull();
        }
    }

    @Test
    void the_chat_model_follows_AI_CHAT_MODEL_including_gemini_3() {
        try (var ctx = start("GEMINI_API_KEY=AIza-test", "AI_CHAT_MODEL=gemini-3.5-flash-lite")) {
            assertThat(ctx.getBean(GoogleGenAiChatProperties.class).getOptions().getModel()).isEqualTo("gemini-3.5-flash-lite");
        }
    }

    @Test
    void enabled_with_an_empty_key_still_starts() {
        try (var ctx = start("GEMINI_API_KEY=", "AI_ENABLED=true")) {
            assertThat(ctx.getBeansOfType(Client.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(ChatClient.class)).hasSize(2);
        }
    }

    @Test
    void chat_can_be_switched_to_ollama_or_openai_with_environment_variables_only() {
        try (var ctx = start("GEMINI_API_KEY=ollama", "AI_CHAT_PROVIDER=openai", "AI_BASE_URL=http://localhost:11434",
                "AI_CHAT_PATH=/v1/chat/completions", "AI_EMBEDDINGS_PATH=/v1/embeddings",
                "AI_CHAT_MODEL=qwen2.5", "AI_EMBEDDING_MODEL=bge-m3", "AI_EMBEDDING_DIMENSIONS=1024")) {
            assertThat(ctx.getBeansOfType(OpenAiChatModel.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(GoogleGenAiChatModel.class)).isEmpty();
            assertThat(ctx.getBeansOfType(Client.class)).isEmpty();
            assertThat(ctx.getBean(OpenAiConnectionProperties.class).getBaseUrl()).isEqualTo("http://localhost:11434");
            assertThat(ctx.getBean(OpenAiChatProperties.class).getOptions().getModel()).isEqualTo("qwen2.5");
            assertThat(ctx.getBean(OpenAiEmbeddingProperties.class).getOptions().getDimensions()).isEqualTo(1024);
        }
    }
}
