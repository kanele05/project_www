package vn.edu.iuh.fit.tourbooking.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.edu.iuh.fit.tourbooking.dto.ApiError;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

import java.util.List;

/**
 * Quy mọi ngoại lệ của tầng web service về một khuôn phản hồi JSON thống nhất.
 *
 * <p><b>Vì sao phải giới hạn {@code basePackages}:</b> nếu để lớp này bắt lỗi
 * toàn cục thì các trang HTML cũng nhận JSON - người dùng đang xem website bỗng
 * thấy một khối chữ {@code {"status":404,...}} thay vì trang báo lỗi. Khu vực
 * giao diện có bộ xử lý riêng ({@code GlobalExceptionHandler}).</p>
 *
 * <p><b>Bảng quy đổi và lý do:</b></p>
 * <ul>
 *   <li>{@link ResourceNotFoundException} &rarr; <b>404</b>: hỏi một bản ghi
 *       không tồn tại là lỗi của phía gọi, không phải hỏng máy chủ.</li>
 *   <li>{@link BusinessRuleException} &rarr; <b>409 Conflict</b>: yêu cầu hợp lệ
 *       về cú pháp nhưng xung đột với trạng thái hiện tại của dữ liệu, ví dụ xoá
 *       một danh mục đang còn tour. <b>Đây chính là bằng chứng rõ nhất rằng ràng
 *       buộc được kiểm tra trong chương trình chứ không phải do CSDL</b>: nếu để
 *       khoá ngoại chặn thì phản hồi sẽ là 500 kèm một câu lỗi SQL khó hiểu.</li>
 *   <li>Dữ liệu nhập sai &rarr; <b>400</b> kèm danh sách từng ô sai.</li>
 * </ul>
 */
@RestControllerAdvice(basePackages = "vn.edu.iuh.fit.tourbooking.controller.api")
@RequiredArgsConstructor
@Slf4j
public class ApiExceptionHandler {

    private final MessageHelper messages;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException e,
                                                   HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "error.notFound", e.getMessage(), request);
    }

    /**
     * Vi phạm quy tắc nghiệp vụ.
     *
     * <p>Câu chữ được tra từ {@code messages.properties} theo ngôn ngữ của yêu
     * cầu hiện tại, nên phản hồi JSON cũng đổi ngữ theo giao diện. Mã khoá được
     * trả kèm ở trường {@code code} để phía trình duyệt phân biệt loại lỗi mà
     * không phải so khớp chuỗi.</p>
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException e,
                                                       HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, e.getMessageKey(), messages.of(e), request);
    }

    /** Lỗi kiểm tra dữ liệu của một đối tượng gắn {@code @Valid}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e,
                                                     HttpServletRequest request) {
        List<ApiError.FieldError> fields = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST, "error.validation",
                messages.get("error.api.validation"), request.getRequestURI(), fields));
    }

    /** Lỗi kiểm tra dữ liệu của từng tham số rời (ví dụ {@code @Min} trên {@code @RequestParam}). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParamValidation(HandlerMethodValidationException e,
                                                          HttpServletRequest request) {
        // getParameterValidationResults() chứ không phải getAllValidationResults():
        // bản kia đã bị đánh dấu loại bỏ ở Spring 6.2 và sẽ biến mất ở bản sau.
        List<ApiError.FieldError> fields = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ApiError.FieldError(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();

        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST, "error.validation",
                messages.get("error.api.validation"), request.getRequestURI(), fields));
    }

    /**
     * Tham số sai kiểu, ví dụ {@code /api/tours/abc} trong khi máy chủ chờ một số.
     *
     * <p>Không để rơi xuống bộ xử lý 500: đây là lỗi của phía gọi.</p>
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                       HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "error.api.badParameter",
                messages.get("error.api.badParameter", e.getName()), request);
    }

    /**
     * Thiếu quyền.
     *
     * <p>Chỉ bắt được các trường hợp do chú thích trên phương thức ném ra. Việc
     * chặn ở tầng bộ lọc của Spring Security xảy ra trước cả DispatcherServlet
     * nên không đi qua đây - đó là lý do {@code SecurityConfig} còn phải khai báo
     * riêng bộ trả lời JSON cho {@code /api/**}.</p>
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException e,
                                                       HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "error.api.forbidden",
                messages.get("error.api.forbidden"), request);
    }

    /**
     * Lưới an toàn cuối cùng.
     *
     * <p>Ghi lại toàn bộ vết lỗi vào log nhưng <b>không</b> gửi ra ngoài: vết lỗi
     * để lộ tên lớp, tên bảng và cấu trúc thư mục của máy chủ.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Lỗi không lường trước tại {}", request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "error.api.internal",
                messages.get("error.api.internal"), request);
    }

    // ---------------------------------------------------------------------

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status, code, message, request.getRequestURI()));
    }
}
