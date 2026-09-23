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

@Configuration
// Cấu hình song ngữ VI/EN: chọn ngôn ngữ lưu bằng cookie, đổi ngôn ngữ qua tham số ?lang=.
public class I18nConfig implements WebMvcConfigurer {

    public static final String LANG_PARAM = "lang";

    public static final String LANG_COOKIE = "APP_LANG";

    public static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
    public static final Locale ENGLISH = Locale.ENGLISH;

    public static final List<Locale> SUPPORTED = List.of(VIETNAMESE, ENGLISH);

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver(LANG_COOKIE);

        resolver.setDefaultLocale(VIETNAMESE);

        resolver.setCookieMaxAge(Duration.ofDays(365));

        resolver.setCookiePath("/");

        resolver.setCookieHttpOnly(true);
        return resolver;
    }

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
