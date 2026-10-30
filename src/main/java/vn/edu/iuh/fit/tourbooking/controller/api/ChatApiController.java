package vn.edu.iuh.fit.tourbooking.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.tourbooking.ai.AiChatService;
import vn.edu.iuh.fit.tourbooking.dto.form.ChatRequest;
import vn.edu.iuh.fit.tourbooking.dto.view.ChatReply;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
// REST cho khung chat "Trợ lý tour": nhận một tin nhắn, trả câu trả lời; AI tắt thì trả 503 kèm câu thân thiện.
public class ChatApiController {

    private final AiChatService chatService;

    @PostMapping
    public ChatReply ask(@Valid @RequestBody ChatRequest request, HttpSession session,
                         @AuthenticationPrincipal CustomUserDetails principal) {
        return new ChatReply(chatService.reply(request.message(), session,
                principal == null ? null : principal.getId()));
    }
}
