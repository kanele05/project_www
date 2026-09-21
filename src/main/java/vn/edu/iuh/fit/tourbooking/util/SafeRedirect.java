package vn.edu.iuh.fit.tourbooking.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Dựng đường dẫn "quay lại trang cũ" từ tiêu đề {@code Referer}, sau khi đã
 * kiểm tra kỹ.
 *
 * <p><b>Vì sao không dùng thẳng giá trị của {@code Referer}:</b> đó là dữ liệu do
 * phía yêu cầu tự khai, ai cũng đặt được. Nhận bừa thì địa chỉ nào chuyển hướng
 * theo nó cũng thành một cái <i>máy chuyển hướng mở</i>: kẻ xấu gửi cho nạn nhân
 * một đường dẫn của <b>chính website này</b> kèm Referer trỏ sang trang giả mạo;
 * nạn nhân thấy tên miền quen nên bấm, rồi bị đưa đi nơi khác.</p>
 *
 * <p>Vì vậy: chỉ nhận Referer cùng máy chủ, và chỉ giữ lại phần đường dẫn cùng
 * tham số truy vấn - phần tên miền không bao giờ được dùng lại.</p>
 *
 * <p>Gom vào một lớp dùng chung vì cả trang đổi ngôn ngữ lẫn bộ xử lý lỗi đều
 * cần đúng phép kiểm tra này; chép làm hai bản thì sớm muộn cũng có một bản bị
 * sửa lỏng ra.</p>
 */
public final class SafeRedirect {

    private SafeRedirect() {
    }

    /**
     * @param fallback đường dẫn dùng khi không xác định được nơi quay lại,
     *                 thường là {@code "/"}
     * @return đường dẫn tương đối an toàn, luôn bắt đầu bằng một dấu gạch chéo
     */
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

        // Referer tuyệt đối thì tên máy chủ phải trùng máy chủ đang phục vụ.
        if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) {
            return fallback;
        }

        String path = uri.getRawPath();
        if (path == null || path.isBlank()) {
            return fallback;
        }

        // "//ten-mien-khac.com" là đường dẫn tương đối giao thức: trình duyệt hiểu
        // đó là một máy chủ khác chứ không phải một thư mục trên máy chủ này.
        if (!path.startsWith("/") || path.startsWith("//")) {
            return fallback;
        }

        // Quay lại chính địa chỉ đang xử lý thì thành vòng lặp chuyển hướng.
        if (path.equals(request.getRequestURI())) {
            return fallback;
        }

        String query = uri.getRawQuery();
        // Giữ nguyên tham số truy vấn: quay lại /tours?q=hue&page=2 phải ra đúng
        // trang 2 với đúng từ khoá đó.
        return query == null || query.isBlank() ? path : path + "?" + query;
    }
}
