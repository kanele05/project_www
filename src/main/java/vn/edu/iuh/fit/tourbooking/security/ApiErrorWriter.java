package vn.edu.iuh.fit.tourbooking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import vn.edu.iuh.fit.tourbooking.dto.ApiError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Ghi một phản hồi lỗi JSON thẳng ra {@code HttpServletResponse}.
 *
 * <p>Cần lớp riêng vì các bộ lọc của Spring Security chạy <b>trước</b>
 * DispatcherServlet: lúc chúng chặn một yêu cầu thì chưa có controller nào được
 * gọi, nên {@code ApiExceptionHandler} không hề biết tới. Không có lớp này thì
 * một lời gọi {@code /api/admin/...} khi chưa đăng nhập sẽ nhận về <b>trang HTML
 * đăng nhập kèm mã 302</b> - phía AJAX đọc được một mớ thẻ HTML và không hiểu
 * chuyện gì đang xảy ra.</p>
 *
 * <p>Dùng lại {@code ObjectMapper} do Spring Boot cấu hình sẵn (đã đăng ký module
 * xử lý kiểu ngày giờ của Java) thay vì tự {@code new} một cái mới.</p>
 */
@Component
@RequiredArgsConstructor
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    /**
     * Bộ giải ngôn ngữ, dùng trực tiếp thay vì qua {@code LocaleContextHolder}.
     *
     * <p><b>Đây là chỗ rất dễ sai và đã sai thật một lần.</b> Các lớp khác trong
     * dự án tra câu chữ qua {@code MessageHelper}, vốn đọc ngôn ngữ từ
     * {@code LocaleContextHolder}. Nhưng giá trị đó do {@code DispatcherServlet}
     * đặt vào lúc bắt đầu xử lý yêu cầu, mà lớp này lại chạy ở tầng <b>bộ lọc</b> -
     * tức là <i>trước</i> DispatcherServlet. Hệ quả: {@code LocaleContextHolder}
     * còn đang giữ ngôn ngữ mặc định của máy ảo Java, nên máy này chạy tiếng Anh
     * thì người dùng đang xem tiếng Việt vẫn nhận được thông báo lỗi tiếng Anh.
     * Hỏi thẳng {@code LocaleResolver} thì đọc đúng cookie ngôn ngữ của yêu cầu.</p>
     */
    private final LocaleResolver localeResolver;

    /**
     * @param messageKey khoá trong {@code messages.properties}, dùng làm cả mã lỗi
     *                   lẫn nguồn của câu thông báo
     */
    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String messageKey) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Locale locale = localeResolver.resolveLocale(request);
        String message = messageSource.getMessage(messageKey, null, locale);

        ApiError body = ApiError.of(status, messageKey, message, request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * Yêu cầu này có nhắm vào web service không.
     *
     * <p>So sánh chuỗi đường dẫn thay vì dùng {@code AntPathRequestMatcher}: quy
     * tắc chỉ có một dòng, đọc là hiểu, và không phụ thuộc vào lớp nào của Spring
     * Security có thể bị thay tên ở bản sau.</p>
     */
    public static boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }
}
