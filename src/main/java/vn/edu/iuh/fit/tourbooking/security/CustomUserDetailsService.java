package vn.edu.iuh.fit.tourbooking.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.repository.UserRepository;

/**
 * Cầu nối giữa bảng {@code users} và Spring Security.
 *
 * <p>Chỉ cần khai báo lớp này là bean, Spring Boot tự dùng nó thay cho tài khoản
 * mặc định sinh ngẫu nhiên lúc khởi động (dòng "Using generated security
 * password" trong log sẽ biến mất).</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * @param email người dùng nhập ở ô đăng nhập - hệ thống dùng email làm tên
     *              đăng nhập nên tham số {@code username} ở đây chính là email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(CustomUserDetails::new)
                // Thông báo cố tình chung chung và không nói rõ "email không tồn tại":
                // phân biệt hai trường hợp sẽ giúp kẻ tấn công dò được email nào đã
                // đăng ký. Spring Security cũng luôn hiện cùng một câu cho mọi lỗi
                // đăng nhập vì lý do đó.
                .orElseThrow(() -> new UsernameNotFoundException("Sai thông tin đăng nhập"));
    }
}
