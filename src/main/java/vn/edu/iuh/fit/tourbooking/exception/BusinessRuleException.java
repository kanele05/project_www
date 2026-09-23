package vn.edu.iuh.fit.tourbooking.exception;

import lombok.Getter;

@Getter
// Lỗi vi phạm quy tắc nghiệp vụ (tầng service ném ra), mang theo khoá câu chữ + tham số để dịch đúng ngôn ngữ.
public class BusinessRuleException extends RuntimeException {

    private final String messageKey;

    private final transient Object[] args;

    public BusinessRuleException(String messageKey, Object... args) {

        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }
}
