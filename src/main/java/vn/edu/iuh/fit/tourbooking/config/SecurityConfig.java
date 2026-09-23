package vn.edu.iuh.fit.tourbooking.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.session.SimpleRedirectSessionInformationExpiredStrategy;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import vn.edu.iuh.fit.tourbooking.security.ApiAccessDeniedHandler;
import vn.edu.iuh.fit.tourbooking.security.ApiAuthenticationEntryPoint;
import vn.edu.iuh.fit.tourbooking.security.ApiErrorWriter;
import vn.edu.iuh.fit.tourbooking.security.LoginSuccessHandler;

@Configuration
@RequiredArgsConstructor
// Cấu hình Spring Security: phân quyền theo đường dẫn, đăng nhập/đăng xuất, và đăng ký phiên để có thể huỷ từ xa khi admin đổi quyền/khoá tài khoản.
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // Khai chuỗi filter chính: quy tắc phân quyền theo đường dẫn, form login, logout, remember-me, quản lý phiên và xử lý ngoại lệ 401/403.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/uploads/**", "/favicon.ico").permitAll()

                        .requestMatchers("/", "/about", "/tours/**", "/categories/**",
                                "/cart/**", "/contact", "/register", "/login").permitAll()

                        .requestMatchers("/change-language").permitAll()

                        .requestMatchers("/error").permitAll()

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/tours/**", "/api/categories/**",
                                "/api/users/email-available").permitAll()
                        .requestMatchers("/api/cart/**", "/api/cart").permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/checkout/**", "/account/**").authenticated()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")

                        .usernameParameter("email")
                        .passwordParameter("password")

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

                        .key("tourbooking-remember-me-iuh")
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)

                        .sessionFixation(sf -> sf.changeSessionId())

                        .sessionConcurrency(concurrency -> concurrency
                                .sessionRegistry(sessionRegistry())
                                .maximumSessions(-1)

                                .expiredSessionStrategy(new SimpleRedirectSessionInformationExpiredStrategy(
                                        "/login?expired")))
                )
                .exceptionHandling(ex -> ex

                        .defaultAuthenticationEntryPointFor(
                                apiAuthenticationEntryPoint, ApiErrorWriter::isApiRequest)

                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                AnyRequestMatcher.INSTANCE)

                        .accessDeniedHandler(apiAccessDeniedHandler)
                );

        return http.build();
    }
}
