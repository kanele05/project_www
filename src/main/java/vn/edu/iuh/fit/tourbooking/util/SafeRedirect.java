package vn.edu.iuh.fit.tourbooking.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.net.URISyntaxException;

// Suy ra đường dẫn quay lại an toàn từ header Referer, chặn mọi khả năng thoát ra ngoài miền hoặc mở đường dẫn lạ.
public final class SafeRedirect {

    private SafeRedirect() {
    }

    // Trả về path (+query) của Referer nếu hợp lệ và cùng miền; ngược lại dùng fallback.
    public static String refererPath(HttpServletRequest request, String fallback) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.isBlank()) {
            return fallback;
        }

        URI uri;
        try {
            uri = new URI(referer);
        } catch (URISyntaxException e) {
            return fallback;
        }

        if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) {
            return fallback;
        }

        String path = uri.getRawPath();
        if (path == null || path.isBlank()) {
            return fallback;
        }

        if (!path.startsWith("/") || path.startsWith("//")) {
            return fallback;
        }

        if (path.equals(request.getRequestURI())) {
            return fallback;
        }

        String query = uri.getRawQuery();

        return query == null || query.isBlank() ? path : path + "?" + query;
    }
}
