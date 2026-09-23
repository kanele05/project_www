package vn.edu.iuh.fit.tourbooking.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.repository.UserRepository;

@Service
@RequiredArgsConstructor
// Nạp người dùng theo email cho Spring Security lúc đăng nhập.
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(CustomUserDetails::new)

                .orElseThrow(() -> new UsernameNotFoundException("Sai thông tin đăng nhập"));
    }
}
