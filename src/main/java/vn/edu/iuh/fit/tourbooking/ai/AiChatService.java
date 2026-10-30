package vn.edu.iuh.fit.tourbooking.ai;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.tourbooking.config.AiProperties;
import vn.edu.iuh.fit.tourbooking.exception.AiUnavailableException;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.RateLimitExceededException;
import vn.edu.iuh.fit.tourbooking.service.TrackingService;
import vn.edu.iuh.fit.tourbooking.util.SlidingWindowRateLimiter;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
// Trợ lý tour: gọi ChatClient (system prompt + bộ nhớ hội thoại + RAG + tool) theo từng phiên; kiểm độ dài, giới hạn tốc độ, bắt lỗi nhà cung cấp.
// Vòng gọi công cụ bị nhà cung cấp từ chối bằng 4xx (vd. Gemini 400 thiếu thought_signature) thì thử lại MỘT lần không công cụ (chỉ RAG).
public class AiChatService {

    // Tất cả 4xx đều đáng thử lại không công cụ, trừ ba mã mà bỏ công cụ cũng không cứu được: khoá sai/hết quyền (401, 403) và hết hạn mức (429).
    private static final java.util.Set<Integer> NO_FALLBACK_STATUS = java.util.Set.of(401, 403, 429);
    private static final Pattern HTTP_STATUS = Pattern.compile("^\\s*(?:HTTP\\s+)?(4\\d\\d)\\s+-");

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final ObjectProvider<ChatClient> ragOnlyProvider;
    private final ObjectProvider<ChatMemory> memoryProvider;
    private final AiProperties properties;
    private final SlidingWindowRateLimiter limiter;

    public AiChatService(ObjectProvider<ChatClient> chatClientProvider,
                         @Qualifier("tourAssistantRagOnly") ObjectProvider<ChatClient> ragOnlyProvider,
                         ObjectProvider<ChatMemory> memoryProvider,
                         AiProperties properties) {
        this.chatClientProvider = chatClientProvider;
        this.ragOnlyProvider = ragOnlyProvider;
        this.memoryProvider = memoryProvider;
        this.properties = properties;
        this.limiter = new SlidingWindowRateLimiter(properties.chatPerMinute(), Duration.ofMinutes(1));
    }

    public boolean isAvailable() {
        return chatClientProvider.getIfAvailable() != null;
    }

    // Trả lời một tin nhắn của khách; conversationId là mã phiên đã băm nên mỗi phiên có lịch sử riêng.
    public String reply(String message, HttpSession session, Long userId) {
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            throw new AiUnavailableException("error.ai.disabled");
        }
        String text = message == null ? "" : message.strip();
        if (text.isEmpty()) {
            throw new BusinessRuleException("error.ai.emptyMessage");
        }
        if (text.length() > properties.maxMessageLength()) {
            throw new BusinessRuleException("error.ai.messageTooLong", properties.maxMessageLength());
        }

        String conversationId = TrackingService.hashSession(session.getId());
        if (!limiter.tryAcquire(conversationId)) {
            throw new RateLimitExceededException("error.ai.rateLimited");
        }

        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put(TourTools.CTX_SESSION, session);
        if (userId != null) {
            toolContext.put(TourTools.CTX_USER_ID, userId);
        }

        ChatMemory memory = memoryProvider.getIfAvailable();
        List<Message> memoryBefore = memory == null ? List.of() : List.copyOf(memory.get(conversationId));

        try {
            return ask(chatClient, text, conversationId, toolContext);
        } catch (RuntimeException e) {
            ChatClient ragOnly = ragOnlyProvider.getIfAvailable();
            if (ragOnly == null || ragOnly == chatClient || !shouldFallBackWithoutTools(e)) {
                log.warn("Gọi mô hình AI thất bại: {}", e.toString());
                throw new AiUnavailableException("error.ai.unavailable", e);
            }
            log.warn("Vòng gọi công cụ bị nhà cung cấp từ chối, thử lại một lần KHÔNG công cụ (chỉ RAG): {}", e.toString());
            restoreMemory(memory, conversationId, memoryBefore);
            try {
                return ask(ragOnly, text, conversationId, toolContext);
            } catch (RuntimeException second) {
                log.warn("Thử lại không công cụ cũng thất bại: {}", second.toString());
                throw new AiUnavailableException("error.ai.unavailable", second);
            }
        }
    }

    private String ask(ChatClient client, String text, String conversationId, Map<String, Object> toolContext) {
        String answer = client.prompt()
                .user(text)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(toolContext)
                .call()
                .content();
        return answer == null || answer.isBlank() ? "..." : answer;
    }

    // Advisor bộ nhớ đã ghi tin nhắn của khách trước khi gọi mô hình; lần thử lại sẽ ghi lần nữa nên phải trả lịch sử về như cũ.
    private static void restoreMemory(ChatMemory memory, String conversationId, List<Message> before) {
        if (memory == null) {
            return;
        }
        memory.clear(conversationId);
        if (!before.isEmpty()) {
            memory.add(conversationId, before);
        }
    }

    static boolean shouldFallBackWithoutTools(Throwable error) {
        Integer status = clientErrorStatus(error);
        return status != null && !NO_FALLBACK_STATUS.contains(status);
    }

    // Mã 4xx của nhà cung cấp: lỗi của Google GenAI SDK mang mã trong ApiException; đường OpenAI-compat của Spring AI ghi "400 - ..." hoặc "HTTP 400 - ..." ở đầu message (tuỳ cấu hình retry).
    static Integer clientErrorStatus(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof com.google.genai.errors.ApiException api) {
                return api.code() >= 400 && api.code() < 500 ? api.code() : null;
            }
            if (t instanceof org.springframework.ai.retry.NonTransientAiException) {
                Matcher m = HTTP_STATUS.matcher(String.valueOf(t.getMessage()));
                if (m.find()) {
                    return Integer.valueOf(m.group(1));
                }
            }
        }
        return null;
    }
}
