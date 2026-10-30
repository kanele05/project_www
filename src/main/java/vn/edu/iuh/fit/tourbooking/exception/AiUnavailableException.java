package vn.edu.iuh.fit.tourbooking.exception;

// Tính năng AI chưa cấu hình hoặc nhà cung cấp mô hình không phản hồi (trả về HTTP 503, kèm câu thân thiện).
public class AiUnavailableException extends RuntimeException {

    private final String messageKey;

    public AiUnavailableException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public AiUnavailableException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
