package vn.edu.iuh.fit.tourbooking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
// Khuôn JSON lỗi chuẩn cho mọi phản hồi REST /api/**.
public record ApiError(LocalDateTime timestamp,
                       int status,
                       String error,
                       String code,
                       String message,
                       String path,
                       List<FieldError> fieldErrors) {

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
