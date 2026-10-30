package vn.edu.iuh.fit.tourbooking.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.mock.web.MockHttpSession;
import vn.edu.iuh.fit.tourbooking.config.AiProperties;
import vn.edu.iuh.fit.tourbooking.exception.AiUnavailableException;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.RateLimitExceededException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiChatServiceTest {

    private static class FakeChatModel implements ChatModel {
        final List<Prompt> prompts = new ArrayList<>();
        boolean fail;
        RuntimeException failure;

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            if (failure != null) {
                throw failure;
            }
            if (fail) {
                throw new IllegalStateException("401 Unauthorized");
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Chào bạn, mình giúp được gì?"))));
        }
    }

    private AiProperties props(int perMinute) {
        AiProperties base = AiTestSupport.props(Path.of("unused"), "m");
        return new AiProperties(true, "k", base.baseUrl(), base.chatModel(), base.embeddingModel(), null, 0L,
                base.vectorStorePath(), 0.1, 30, 4, 20, perMinute, 50);
    }

    private AiChatService service(FakeChatModel model, int perMinute) {
        var memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository()).maxMessages(20).build();
        ChatClient client = ChatClient.builder(model)
                .defaultSystem(AiConfigAccess.systemPrompt())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
        return new AiChatService(AiTestSupport.provider(ChatClient.class, client),
                AiTestSupport.provider(ChatClient.class, null), AiTestSupport.provider(ChatMemory.class, memory),
                props(perMinute));
    }

    @Test
    void answers_with_system_prompt_and_remembers_the_conversation_per_session() {
        var model = new FakeChatModel();
        var chat = service(model, 10);
        var session = new MockHttpSession();

        assertThat(chat.reply("Xin chào", session, null)).isEqualTo("Chào bạn, mình giúp được gì?");
        chat.reply("Tour biển nào rẻ?", session, null);

        Prompt second = model.prompts.get(1);
        List<String> texts = second.getInstructions().stream().map(Message::getText).toList();
        assertThat(texts).anyMatch(t -> t.contains("Trợ lý tour"));
        assertThat(texts).contains("Xin chào").contains("Tour biển nào rẻ?");

        chat.reply("Còn chỗ không?", new MockHttpSession(), null);
        List<String> otherSession = model.prompts.get(2).getInstructions().stream().map(Message::getText).toList();
        assertThat(otherSession).doesNotContain("Xin chào");
    }

    @Test
    void ai_disabled_gives_a_friendly_unavailable_exception() {
        var chat = new AiChatService(AiTestSupport.provider(ChatClient.class, null),
                AiTestSupport.provider(ChatClient.class, null), AiTestSupport.provider(ChatMemory.class, null), props(10));
        assertThat(chat.isAvailable()).isFalse();
        assertThatThrownBy(() -> chat.reply("hi", new MockHttpSession(), null))
                .isInstanceOf(AiUnavailableException.class)
                .extracting("messageKey").isEqualTo("error.ai.disabled");
    }

    @Test
    void validates_empty_and_oversized_messages() {
        var chat = service(new FakeChatModel(), 10);
        assertThatThrownBy(() -> chat.reply("   ", new MockHttpSession(), null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> chat.reply("x".repeat(51), new MockHttpSession(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rate_limits_a_chatty_session() {
        var chat = service(new FakeChatModel(), 2);
        var session = new MockHttpSession();
        chat.reply("một", session, null);
        chat.reply("hai", session, null);
        assertThatThrownBy(() -> chat.reply("ba", session, null)).isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void provider_errors_are_translated_without_leaking_details() {
        var model = new FakeChatModel();
        model.fail = true;
        var chat = service(model, 10);
        assertThatThrownBy(() -> chat.reply("hi", new MockHttpSession(), null))
                .isInstanceOf(AiUnavailableException.class)
                .extracting("messageKey").isEqualTo("error.ai.unavailable");
    }

    // ---- Dự phòng không công cụ khi nhà cung cấp từ chối vòng gọi công cụ bằng 4xx ----

    private static final String GEMINI_400 = "HTTP 400 - [{\"error\":{\"code\":400,\"message\":\"Function call is missing a thought_signature in functionCall parts.\"}}]";

    private record Wired(AiChatService service, org.springframework.ai.chat.memory.ChatMemory memory) {
    }

    private Wired withFallback(FakeChatModel withTools, FakeChatModel ragOnly) {
        var memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository()).maxMessages(20).build();
        ChatClient primary = ChatClient.builder(withTools).defaultSystem("CÓ CÔNG CỤ")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build()).build();
        ChatClient fallback = ChatClient.builder(ragOnly).defaultSystem("KHÔNG CÔNG CỤ")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build()).build();
        return new Wired(new AiChatService(AiTestSupport.provider(ChatClient.class, primary),
                AiTestSupport.provider(ChatClient.class, fallback), AiTestSupport.provider(ChatMemory.class, memory),
                props(10)), memory);
    }

    private ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> captureLog() {
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(AiChatService.class)).addAppender(appender);
        return appender;
    }

    @Test
    void a_4xx_on_the_tool_round_retries_once_without_tools_logs_a_warning_and_keeps_history_clean() {
        var withTools = new FakeChatModel();
        withTools.failure = new org.springframework.ai.retry.NonTransientAiException(GEMINI_400);
        var ragOnly = new FakeChatModel();
        var wired = withFallback(withTools, ragOnly);
        var log = captureLog();
        var session = new MockHttpSession();

        String answer = wired.service().reply("Tour biển nào rẻ?", session, null);

        assertThat(answer).isEqualTo("Chào bạn, mình giúp được gì?");
        assertThat(withTools.prompts).hasSize(1);
        assertThat(ragOnly.prompts).hasSize(1);
        assertThat(ragOnly.prompts.get(0).getInstructions().stream().map(Message::getText))
                .anyMatch(t -> t.contains("KHÔNG CÔNG CỤ"));
        // Tin nhắn của khách chỉ xuất hiện một lần trong yêu cầu thử lại và trong bộ nhớ (không bị ghi trùng).
        assertThat(ragOnly.prompts.get(0).getInstructions().stream().map(Message::getText)
                .filter("Tour biển nào rẻ?"::equals)).hasSize(1);
        String conversationId = vn.edu.iuh.fit.tourbooking.service.TrackingService.hashSession(session.getId());
        assertThat(wired.memory().get(conversationId).stream().map(Message::getText))
                .containsExactly("Tour biển nào rẻ?", "Chào bạn, mình giúp được gì?");
        assertThat(log.list).anyMatch(e -> e.getLevel() == ch.qos.logback.classic.Level.WARN
                && e.getFormattedMessage().contains("KHÔNG công cụ") && e.getFormattedMessage().contains("thought_signature"));
    }

    @Test
    void the_google_genai_sdk_error_type_triggers_the_same_fallback() {
        var withTools = new FakeChatModel();
        withTools.failure = new RuntimeException("Failed to generate content",
                new com.google.genai.errors.ClientException(400, "INVALID_ARGUMENT", "Function call is missing a thought_signature"));
        var ragOnly = new FakeChatModel();
        assertThat(withFallback(withTools, ragOnly).service().reply("xin chào", new MockHttpSession(), null)).isNotBlank();
        assertThat(ragOnly.prompts).hasSize(1);
    }

    @Test
    void errors_that_dropping_tools_cannot_fix_do_not_trigger_the_fallback() {
        java.util.List<RuntimeException> errors = java.util.List.of(
                new org.springframework.ai.retry.NonTransientAiException("HTTP 401 - bad key"),
                new org.springframework.ai.retry.NonTransientAiException("HTTP 429 - quota"),
                new RuntimeException("x", new com.google.genai.errors.ClientException(403, "PERMISSION_DENIED", "no")),
                new RuntimeException("x", new com.google.genai.errors.ClientException(429, "RESOURCE_EXHAUSTED", "quota")),
                new RuntimeException("x", new com.google.genai.errors.ServerException(500, "INTERNAL", "boom")),
                new org.springframework.ai.retry.TransientAiException("HTTP 500 - boom"),
                new IllegalStateException("connection reset"));
        for (RuntimeException error : errors) {
            var withTools = new FakeChatModel();
            withTools.failure = error;
            var ragOnly = new FakeChatModel();
            var chat = withFallback(withTools, ragOnly).service();
            assertThatThrownBy(() -> chat.reply("xin chào", new MockHttpSession(), null))
                    .as(error.toString()).isInstanceOf(AiUnavailableException.class);
            assertThat(ragOnly.prompts).as(error.toString()).isEmpty();
        }
    }

    @Test
    void if_the_retry_without_tools_also_fails_the_user_gets_the_friendly_unavailable_error() {
        var withTools = new FakeChatModel();
        withTools.failure = new org.springframework.ai.retry.NonTransientAiException(GEMINI_400);
        var ragOnly = new FakeChatModel();
        ragOnly.failure = new org.springframework.ai.retry.NonTransientAiException("HTTP 400 - again");
        var wired = withFallback(withTools, ragOnly);
        assertThatThrownBy(() -> wired.service().reply("xin chào", new MockHttpSession(), null))
                .isInstanceOf(AiUnavailableException.class).extracting("messageKey").isEqualTo("error.ai.unavailable");
        assertThat(ragOnly.prompts).hasSize(1);
    }
}
