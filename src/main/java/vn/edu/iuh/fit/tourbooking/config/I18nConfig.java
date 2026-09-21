package vn.edu.iuh.fit.tourbooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Đa ngôn ngữ Việt / Anh.
 *
 * <p><b>Vì sao nhớ ngôn ngữ bằng cookie chứ không bằng session:</b>
 * {@code SessionLocaleResolver} nghe có vẻ gọn hơn, nhưng Spring Security huỷ
 * session khi đăng xuất - người dùng đang xem tiếng Anh, bấm Đăng xuất một cái
 * là cả website nhảy về tiếng Việt. Cookie sống độc lập với phiên nên qua cả
 * đăng xuất lẫn khởi động lại máy chủ vẫn còn.</p>
 *
 * <p>Không cần khai báo {@code MessageSource}: Spring Boot đã tự dựng sẵn từ
 * nhóm cấu hình {@code spring.messages.*} trong {@code application.yml}
 * (basename {@code messages}, mã hoá UTF-8). {@code ValidationConfig} từ giai
 * đoạn trước đã nối bộ thông điệp ấy vào Bean Validation, nên câu báo lỗi nhập
 * liệu cũng tự chuyển ngữ - không phải làm gì thêm.</p>
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /** Tên tham số đổi ngôn ngữ, ví dụ {@code /tours?lang=en}. */
    public static final String LANG_PARAM = "lang";

    /** Tên cookie ghi nhớ lựa chọn. */
    public static final String LANG_COOKIE = "APP_LANG";

    public static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
    public static final Locale ENGLISH = Locale.ENGLISH;

    /** Danh sách ngôn ngữ hỗ trợ - dùng để dựng menu chọn ngôn ngữ. */
    public static final List<Locale> SUPPORTED = List.of(VIETNAMESE, ENGLISH);

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver(LANG_COOKIE);

        // Mặc định tiếng Việt, KHÔNG theo ngôn ngữ trình duyệt: đây là website
        // của một công ty du lịch Việt Nam, khách vào lần đầu phải thấy tiếng Việt
        // dù trình duyệt của họ cài tiếng gì.
        resolver.setDefaultLocale(VIETNAMESE);

        resolver.setCookieMaxAge(Duration.ofDays(365));
        // Không đặt đường dẫn thì cookie chỉ có hiệu lực ở đúng thư mục vừa ghi,
        // đổi ngôn ngữ ở /tours xong sang /cart lại về tiếng Việt.
        resolver.setCookiePath("/");
        // Ngôn ngữ không phải thứ JavaScript cần đọc, đóng lại cho gọn bề mặt.
        resolver.setCookieHttpOnly(true);
        return resolver;
    }

    /**
     * Cho phép đổi ngôn ngữ bằng tham số {@code ?lang=} trên <b>bất kỳ</b> địa chỉ nào.
     *
     * <p>{@code ignoreInvalidLocale(true)} là bắt buộc: mặc định, một giá trị rác
     * như {@code ?lang=xyz!!} sẽ ném ngoại lệ và làm hỏng cả trang. Bỏ qua và giữ
     * nguyên ngôn ngữ cũ là cách xử lý đúng cho một tham số không quan trọng.</p>
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName(LANG_PARAM);
        interceptor.setIgnoreInvalidLocale(true);
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
