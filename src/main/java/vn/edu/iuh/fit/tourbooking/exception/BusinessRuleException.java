package vn.edu.iuh.fit.tourbooking.exception;

import lombok.Getter;

/**
 * Ném ra khi một quy tắc nghiệp vụ bị vi phạm - ví dụ đặt nhiều hơn số chỗ còn
 * lại, hoặc xoá danh mục đang còn tour.
 *
 * <p><b>Mang theo mã thông điệp chứ không mang chuỗi tiếng Việt.</b> Toàn bộ câu
 * chữ nằm trong {@code messages.properties}; nhờ vậy khi bổ sung tiếng Anh ở
 * giai đoạn sau, không phải sửa lại một dòng mã Java nào. Đây cũng là loại lỗi
 * mà tầng REST quy về mã trạng thái <b>409 Conflict</b> - bằng chứng rõ nhất
 * rằng ràng buộc được kiểm tra trong chương trình chứ không phải trong CSDL.</p>
 */
@Getter
public class BusinessRuleException extends RuntimeException {

    /** Khoá tra cứu trong messages.properties, ví dụ {@code error.cart.notEnoughSeats}. */
    private final String messageKey;

    /** Các tham số điền vào chỗ {0}, {1}... của thông điệp. */
    private final transient Object[] args;

    public BusinessRuleException(String messageKey, Object... args) {
        // Thông điệp của Exception (dùng cho log) cố tình để là chính mã khoá,
        // để khi đọc log biết ngay phải tra dòng nào trong messages.properties.
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }
}
