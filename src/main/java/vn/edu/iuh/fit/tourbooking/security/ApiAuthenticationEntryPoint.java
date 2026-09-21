package vn.edu.iuh.fit.tourbooking.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Trả <b>401 kèm JSON</b> khi một lời gọi web service chưa đăng nhập.
 *
 * <p>Hành vi mặc định là chuyển hướng sang trang đăng nhập (302). Với trình duyệt
 * thì đúng, với AJAX thì sai hẳn: {@code $.ajax} tự đi theo chuyển hướng, nhận
 * về trang đăng nhập dạng HTML với mã <b>200</b>, và nhánh xử lý lỗi không bao
 * giờ chạy - lỗi im lặng, khó lần nhất.</p>
 */
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiErrorWriter errorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        errorWriter.write(request, response, HttpStatus.UNAUTHORIZED, "error.api.unauthorized");
    }
}
