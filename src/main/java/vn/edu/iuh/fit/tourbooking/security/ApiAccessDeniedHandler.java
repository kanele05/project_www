package vn.edu.iuh.fit.tourbooking.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý "đã đăng nhập nhưng không đủ quyền".
 *
 * <p>Một bộ xử lý chung cho cả hai khu vực, rẽ nhánh theo đường dẫn:</p>
 * <ul>
 *   <li>{@code /api/**} &rarr; <b>403 kèm JSON</b> để AJAX đọc được;</li>
 *   <li>còn lại &rarr; giao cho bộ xử lý mặc định của Spring Security, nó chuyển
 *       tiếp sang trang lỗi và Spring Boot dựng {@code templates/error/403.html}.</li>
 * </ul>
 *
 * <p>Cũng chính là nơi trả lời khi <b>thiếu token CSRF</b>: với người đã đăng
 * nhập, thiếu token là 403. (Với khách chưa đăng nhập thì lại là 302 về trang
 * đăng nhập, vì Spring Security coi mọi từ chối của tài khoản ẩn danh là "cần
 * đăng nhập trước đã" - đừng vì thấy 302 mà tưởng CSRF chưa bật.)</p>
 */
@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiErrorWriter errorWriter;

    private final AccessDeniedHandler defaultHandler = new AccessDeniedHandlerImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        if (ApiErrorWriter.isApiRequest(request)) {
            errorWriter.write(request, response, HttpStatus.FORBIDDEN, "error.api.forbidden");
            return;
        }
        defaultHandler.handle(request, response, accessDeniedException);
    }
}
