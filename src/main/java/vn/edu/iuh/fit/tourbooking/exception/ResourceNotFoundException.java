package vn.edu.iuh.fit.tourbooking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
// Lỗi không tìm thấy tài nguyên - tự động trả HTTP 404 nhờ @ResponseStatus.
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("Không tìm thấy " + resource + " có mã " + id);
    }
}
