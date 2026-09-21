package vn.edu.iuh.fit.tourbooking.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import vn.edu.iuh.fit.tourbooking.security.ApiAccessDeniedHandler;
import vn.edu.iuh.fit.tourbooking.security.ApiAuthenticationEntryPoint;
import vn.edu.iuh.fit.tourbooking.security.ApiErrorWriter;
import vn.edu.iuh.fit.tourbooking.security.LoginSuccessHandler;

/**
 * Cấu hình bảo mật.
 *
 * <p>Ở giai đoạn đầu lớp này để mở toàn bộ cho tiện phát triển. Nay đã siết lại
 * đúng ba mức phân quyền của đề bài: khách vãng lai, khách hàng đã đăng nhập và
 * quản trị viên.</p>
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;

    /**
     * BCrypt là thuật toán băm mật khẩu được khuyến nghị: có muối ngẫu nhiên và
     * chi phí tính toán cấu hình được, nên chống được tấn công dò từ điển bằng GPU.
     * Không bao giờ lưu mật khẩu dạng thô trong CSDL.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Tài nguyên tĩnh và ảnh upload phải luôn mở, nếu không toàn
                        // bộ giao diện sẽ mất CSS ngay khi bật bảo mật.
                        .requestMatchers("/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/uploads/**", "/favicon.ico").permitAll()

                        // Khu vực công khai: xem tour và dùng giỏ hàng không cần đăng
                        // nhập. Giỏ hàng nằm trong session của chính trình duyệt nên
                        // mở /cart/** không hề làm lộ dữ liệu của ai. /contact (UC021)
                        // cũng mở cho AC01 Khách vãng lai - đúng nghĩa "liên hệ" không
                        // đòi phải có tài khoản.
                        .requestMatchers("/", "/about", "/tours/**", "/categories/**",
                                "/cart/**", "/contact", "/register", "/login").permitAll()

                        // Đổi ngôn ngữ phải mở cho cả khách chưa đăng nhập, nếu không
                        // thì bấm "English" ở trang chủ lại nhảy sang trang đăng nhập.
                        .requestMatchers("/change-language").permitAll()

                        // Trang lỗi phải mở. Từ Spring Security 6, bộ lọc phân quyền
                        // áp dụng cho MỌI kiểu điều phối, kể cả ERROR - quên dòng này
                        // thì mọi lỗi 404/500 biến thành vòng lặp chuyển hướng.
                        .requestMatchers("/error").permitAll()

                        // ---- Web service ------------------------------------------
                        // Khu quản trị của API chặn theo vai trò, y hệt /admin/**.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Phần đọc công khai: mở đúng bằng những trang tương ứng đang
                        // mở. Riêng /api/cart mở cả ghi vì giỏ hàng nằm trong session
                        // của chính người gọi - không đụng được vào dữ liệu của ai.
                        .requestMatchers(HttpMethod.GET, "/api/tours/**", "/api/categories/**",
                                "/api/users/email-available").permitAll()
                        .requestMatchers("/api/cart/**", "/api/cart").permitAll()

                        // ---- Khu vực cần đăng nhập --------------------------------
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/checkout/**", "/account/**").authenticated()

                        // Mặc định là chặn: thêm địa chỉ mới mà quên khai báo thì nó
                        // bị khoá lại, chứ không âm thầm mở toang.
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        // Hệ thống dùng email làm tên đăng nhập nên phải đổi tên
                        // tham số, mặc định của Spring Security là "username".
                        .usernameParameter("email")
                        .passwordParameter("password")
                        // Bộ xử lý riêng: quản trị viên vào thẳng /admin, khách hàng
                        // quay lại trang đang dở (hoặc trang chủ nếu không có).
                        .successHandler(loginSuccessHandler)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .rememberMe(rm -> rm
                        // Khoá ký cho cookie ghi nhớ đăng nhập. Đặt cố định để khởi
                        // động lại ứng dụng không làm mọi người bị đăng xuất; ở hệ
                        // thống thật thì khoá này phải nằm trong biến môi trường.
                        .key("tourbooking-remember-me-iuh")
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)   // 7 ngày
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        // KHAI BÁO TƯỜNG MINH, đừng bỏ đi dù đây đang là mặc định.
                        // changeSessionId() chỉ đổi mã phiên và GIỮ NGUYÊN các thuộc
                        // tính, nên giỏ hàng khách chọn lúc chưa đăng nhập vẫn còn
                        // nguyên sau khi đăng nhập. Nếu ai đó đổi sang newSession()
                        // thì giỏ hàng bốc hơi ngay tại thời điểm đăng nhập - lỗi rất
                        // khó ngờ và chỉ lộ ra giữa buổi trình bày.
                        .sessionFixation(sf -> sf.changeSessionId())
                )
                .exceptionHandling(ex -> ex
                        // Chưa đăng nhập mà gọi web service thì nhận 401 kèm JSON,
                        // thay vì bị đẩy sang trang đăng nhập dạng HTML.
                        .defaultAuthenticationEntryPointFor(
                                apiAuthenticationEntryPoint, ApiErrorWriter::isApiRequest)

                        // ⚠️ Dòng dưới TRÔNG NHƯ THỪA nhưng bắt buộc phải có.
                        // Khi chỉ khai báo đúng MỘT bộ điểm vào,
                        // ExceptionHandlingConfigurer bỏ luôn bộ chọn theo đường dẫn và
                        // dùng bộ ấy cho MỌI yêu cầu - lúc đó gõ một địa chỉ không tồn
                        // tại cũng nhận về JSON 401 thay vì được đưa sang trang đăng
                        // nhập. Khai báo thêm bộ thứ hai khớp mọi yêu cầu buộc Spring
                        // dựng bộ chọn thật, và thứ tự khai báo chính là thứ tự xét:
                        // /api/** xét trước, phần còn lại rơi vào đây.
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                AnyRequestMatcher.INSTANCE)
                        // Đã đăng nhập nhưng thiếu quyền: /api/** nhận 403 kèm JSON,
                        // các trang khác đi tiếp tới templates/error/403.html.
                        .accessDeniedHandler(apiAccessDeniedHandler)
                );

        // CSRF nay đã BẬT (mặc định của Spring Security, chỉ cần không tắt đi).
        // Mọi biểu mẫu trong dự án đều viết th:action="@{...}" nên Thymeleaf tự
        // chèn thẻ ẩn chứa token - không phải sửa một khuôn mẫu nào.
        //
        // CỐ Ý KHÔNG tắt CSRF cho /api/**. Các web service này xác thực bằng chính
        // phiên đăng nhập (cookie JSESSIONID), mà cookie thì trình duyệt tự gửi kèm
        // theo mọi yêu cầu - kể cả yêu cầu do một trang web khác phát ra. Tắt CSRF
        // ở đây là mở đúng lỗ hổng mà nó sinh ra để bịt. Chỉ những API xác thực
        // bằng token gửi trong header (JWT chẳng hạn) mới được phép tắt.
        // Phía trình duyệt lấy token từ thẻ <meta> rồi gắn vào header một lần bằng
        // $.ajaxSetup - xem app.js.
        return http.build();
    }
}
