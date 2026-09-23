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

@Component
@RequiredArgsConstructor
// Từ chối quyền cho /api/** trả JSON, các đường khác vẫn dùng cách xử lý mặc định của Spring Security.
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
