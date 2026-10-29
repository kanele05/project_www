package vn.edu.iuh.fit.tourbooking.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
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
            OpenAiChatAutoConfiguration.class, OpenAiEmbeddingAutoConfiguration.class,
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
    void with_a_key_the_assistant_is_wired_to_gemini_through_the_openai_starter() {
        try (var ctx = start("GEMINI_API_KEY=AIza-test")) {
            assertThat(ctx.getBeansOfType(TourVectorStore.class)).hasSize(1);
            assertThat(ctx.containsBean("tourAssistant")).isTrue();

            OpenAiConnectionProperties connection = ctx.getBean(OpenAiConnectionProperties.class);
            assertThat(connection.getBaseUrl()).isEqualTo("https://generativelanguage.googleapis.com");
            assertThat(connection.getApiKey()).isEqualTo("AIza-test");

            OpenAiChatProperties chat = ctx.getBean(OpenAiChatProperties.class);
            assertThat(chat.getCompletionsPath()).isEqualTo("/v1beta/openai/chat/completions");
            assertThat(chat.getOptions().getModel()).isEqualTo("gemini-2.5-flash");

            OpenAiEmbeddingProperties embedding = ctx.getBean(OpenAiEmbeddingProperties.class);
            assertThat(embedding.getEmbeddingsPath()).isEqualTo("/v1beta/openai/embeddings");
            assertThat(embedding.getOptions().getModel()).isEqualTo("gemini-embedding-001");
            assertThat(embedding.getOptions().getDimensions()).isNull();
        }
    }

    @Test
    void provider_can_be_switched_to_ollama_or_openai_with_environment_variables_only() {
        try (var ctx = start("GEMINI_API_KEY=ollama", "AI_BASE_URL=http://localhost:11434",
                "AI_CHAT_PATH=/v1/chat/completions", "AI_EMBEDDINGS_PATH=/v1/embeddings",
                "AI_CHAT_MODEL=qwen2.5", "AI_EMBEDDING_MODEL=bge-m3", "AI_EMBEDDING_DIMENSIONS=1024")) {
            assertThat(ctx.getBean(OpenAiConnectionProperties.class).getBaseUrl()).isEqualTo("http://localhost:11434");
            assertThat(ctx.getBean(OpenAiChatProperties.class).getOptions().getModel()).isEqualTo("qwen2.5");
            assertThat(ctx.getBean(OpenAiEmbeddingProperties.class).getOptions().getDimensions()).isEqualTo(1024);
        }
    }
}
