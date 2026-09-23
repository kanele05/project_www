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

@Component
@RequiredArgsConstructor
// Ghi một ApiError ra response JSON đúng ngôn ngữ; dùng ở tầng filter nên phải tự hỏi LocaleResolver (LocaleContextHolder chưa có giá trị ở đây).
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    private final LocaleResolver localeResolver;

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

    public static boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }
}
