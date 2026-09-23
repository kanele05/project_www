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

@RestControllerAdvice(basePackages = "vn.edu.iuh.fit.tourbooking.controller.api")
@RequiredArgsConstructor
@Slf4j
// Bắt lỗi cho toàn bộ controller REST (/api/**), luôn trả JSON ApiError kèm mã trạng thái HTTP đúng.
public class ApiExceptionHandler {

    private final MessageHelper messages;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException e,
                                                   HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "error.notFound", e.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException e,
                                                       HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, e.getMessageKey(), messages.of(e), request);
    }

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

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParamValidation(HandlerMethodValidationException e,
                                                          HttpServletRequest request) {

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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                       HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "error.api.badParameter",
                messages.get("error.api.badParameter", e.getName()), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException e,
                                                       HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "error.api.forbidden",
                messages.get("error.api.forbidden"), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Lỗi không lường trước tại {}", request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "error.api.internal",
                messages.get("error.api.internal"), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status, code, message, request.getRequestURI()));
    }
}
