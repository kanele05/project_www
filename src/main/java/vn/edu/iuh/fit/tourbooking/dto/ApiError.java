package vn.edu.iuh.fit.tourbooking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Khuôn dạng chung cho mọi phản hồi lỗi của các web service.
 *
 * <p>Có một khuôn cố định thì phía trình duyệt chỉ cần viết một hàm xử lý lỗi
 * duy nhất: cứ đọc {@code message} là có câu tiếng Việt đưa thẳng lên màn hình,
 * không phải đoán xem lỗi lần này trả về kiểu gì. Nếu không có lớp này, Spring
 * sẽ trả về trang lỗi HTML mặc định - AJAX nhận được một mớ thẻ HTML thay vì
 * JSON và chỉ hiện ra được câu "undefined".</p>
 *
 * <p>{@code fieldErrors} chỉ xuất hiện khi lỗi là do dữ liệu nhập sai; những
 * trường {@code null} hoặc danh sách rỗng bị {@code @JsonInclude} bỏ khỏi JSON
 * cho gọn.</p>
 *
 * @param status mã trạng thái HTTP, lặp lại trong thân phản hồi để tiện đọc log
 * @param error  tên gọi của mã trạng thái, ví dụ {@code Conflict}
 * @param code   mã thông điệp gốc (ví dụ {@code error.tour.delete.inBooking}) -
 *               giúp phía trình duyệt phân biệt được từng loại lỗi mà không phải
 *               so khớp chuỗi tiếng Việt
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(LocalDateTime timestamp,
                       int status,
                       String error,
                       String code,
                       String message,
                       String path,
                       List<FieldError> fieldErrors) {

    /** Một ô nhập bị sai, dùng để tô đỏ đúng ô đó trên biểu mẫu. */
    public record FieldError(String field, String message) {
    }

    public static ApiError of(HttpStatus status, String code, String message, String path) {
        return new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                code, message, path, List.of());
    }

    public static ApiError of(HttpStatus status, String code, String message, String path,
                              List<FieldError> fieldErrors) {
        return new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                code, message, path, fieldErrors);
    }
}
