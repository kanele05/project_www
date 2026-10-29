package vn.edu.iuh.fit.tourbooking.exception;

// Vượt hạn mức số lần gọi cho phép trong một khoảng thời gian (trả về HTTP 429).
public class RateLimitExceededException extends RuntimeException {

    private final String messageKey;

    public RateLimitExceededException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
