package vn.edu.iuh.fit.tourbooking.ai;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.tourbooking.config.AiProperties;
import vn.edu.iuh.fit.tourbooking.exception.AiUnavailableException;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.RateLimitExceededException;
import vn.edu.iuh.fit.tourbooking.service.TrackingService;
import vn.edu.iuh.fit.tourbooking.util.SlidingWindowRateLimiter;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
// Trợ lý tour: gọi ChatClient (system prompt + bộ nhớ hội thoại + RAG + tool) theo từng phiên; kiểm độ dài, giới hạn tốc độ, bắt lỗi nhà cung cấp.
public class AiChatService {

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final AiProperties properties;
    private final SlidingWindowRateLimiter limiter;

    public AiChatService(ObjectProvider<ChatClient> chatClientProvider, AiProperties properties) {
        this.chatClientProvider = chatClientProvider;
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

        try {
            String answer = chatClient.prompt()
                    .user(text)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .toolContext(toolContext)
                    .call()
                    .content();
            return answer == null || answer.isBlank() ? "..." : answer;
        } catch (RuntimeException e) {
            log.warn("Gọi mô hình AI thất bại: {}", e.toString());
            throw new AiUnavailableException("error.ai.unavailable", e);
        }
    }
}
