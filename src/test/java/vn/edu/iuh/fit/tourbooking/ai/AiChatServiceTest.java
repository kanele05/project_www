package vn.edu.iuh.fit.tourbooking.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
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

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
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
        return new AiChatService(AiTestSupport.provider(ChatClient.class, client), props(perMinute));
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
        var chat = new AiChatService(AiTestSupport.provider(ChatClient.class, null), props(10));
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
}
