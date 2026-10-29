package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Tin nhắn khách gửi cho trợ lý tour; độ dài tối đa còn được kiểm lại ở AiChatService theo cấu hình.
public record ChatRequest(@NotBlank @Size(max = 2000) String message) {
}
