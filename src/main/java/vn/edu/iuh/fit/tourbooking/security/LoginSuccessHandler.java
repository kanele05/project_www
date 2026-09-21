package vn.edu.iuh.fit.tourbooking.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Quyết định đưa người dùng đi đâu ngay sau khi đăng nhập.
 *
 * <p>Kế thừa {@link SavedRequestAwareAuthenticationSuccessHandler} để <b>giữ
 * nguyên</b> hành vi quan trọng nhất: nếu người dùng đang định vào một trang cần
 * đăng nhập (ví dụ bấm "Tiến hành đặt tour" rồi bị đẩy sang trang đăng nhập) thì
 * sau khi đăng nhập xong phải quay lại đúng trang đó. Chỉ khi <i>không</i> có
 * việc gì đang dở thì mới áp dụng luật riêng bên dưới.</p>
 *
 * <p>Luật riêng: quản trị viên vào thẳng {@code /admin} thay vì trang chủ - họ
 * đăng nhập để làm việc, không phải để xem tour.</p>
 */
@Component
@Slf4j
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        boolean hasPendingRequest = requestCache.getRequest(request, response) != null;
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (isAdmin && !hasPendingRequest) {
            log.debug("Quản trị viên {} đăng nhập, chuyển vào khu vực quản trị",
                    authentication.getName());
            getRedirectStrategy().sendRedirect(request, response, "/admin");
            return;
        }

        // Mọi trường hợp còn lại giao lại cho lớp cha: nó lo phần quay về trang
        // đang dở, và nếu không có thì về trang chủ.
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
