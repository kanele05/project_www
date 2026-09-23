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

@Component
@Slf4j
// Sau khi đăng nhập: quản trị viên không có trang đang chờ thì vào /admin, còn lại theo hành vi mặc định (quay lại trang trước đó).
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

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
