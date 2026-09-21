package vn.edu.iuh.fit.tourbooking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ném ra khi người dùng yêu cầu một bản ghi không tồn tại, ví dụ
 * {@code /tours/99999}.
 *
 * <p>{@code @ResponseStatus(NOT_FOUND)} khiến Spring trả về đúng mã <b>404</b>
 * thay vì 500. Việc này quan trọng hơn vẻ ngoài: gõ sai địa chỉ là lỗi của phía
 * yêu cầu, không phải lỗi máy chủ, và các công cụ kiểm thử hay đọc mã trạng thái
 * chứ không đọc nội dung trang.</p>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /** Ví dụ: {@code of("Tour", 99999)} → "Không tìm thấy Tour có mã 99999". */
    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("Không tìm thấy " + resource + " có mã " + id);
    }
}
