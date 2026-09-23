package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.dto.form.AdminUserForm;
import vn.edu.iuh.fit.tourbooking.dto.form.ChangePasswordForm;
import vn.edu.iuh.fit.tourbooking.dto.form.ProfileForm;
import vn.edu.iuh.fit.tourbooking.dto.form.RegisterForm;
import vn.edu.iuh.fit.tourbooking.entity.Role;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.tourbooking.repository.BookingRepository;
import vn.edu.iuh.fit.tourbooking.repository.BookingStatusHistoryRepository;
import vn.edu.iuh.fit.tourbooking.repository.ContactMessageRepository;
import vn.edu.iuh.fit.tourbooking.repository.CouponUsageRepository;
import vn.edu.iuh.fit.tourbooking.repository.PasswordResetTokenRepository;
import vn.edu.iuh.fit.tourbooking.repository.ReviewRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserRepository;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
@Slf4j
// Nghiệp vụ tài khoản: đăng ký, hồ sơ, đổi mật khẩu, và quản trị (thêm/sửa/xoá/khoá) với bảy quy tắc chặn xoá.
public class UserService {

    public static final int ADMIN_PAGE_SIZE = 15;

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final BookingStatusHistoryRepository bookingStatusHistoryRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final SessionRegistry sessionRegistry;

    // Đăng ký tài khoản khách hàng mới, phát sự kiện để gửi thư chào mừng sau khi commit.
    @Transactional
    public User register(RegisterForm form) {

        if (userRepository.existsByEmail(form.getEmail())) {
            throw new BusinessRuleException("error.user.emailExists", form.getEmail());
        }

        User user = new User();
        user.setFullName(form.getFullName().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setPhone(form.getPhone());
        user.setAddress(form.getAddress());
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);

        User saved = userRepository.save(user);
        log.info("Đã đăng ký tài khoản mới: {}", saved.getEmail());

        eventPublisher.publishEvent(new UserRegisteredEvent(saved.getFullName(), saved.getEmail()));
        return saved;
    }

    public record UserRegisteredEvent(String fullName, String email) {
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("người dùng", id));
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email, Long excludeId) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalized = email.trim().toLowerCase();
        return excludeId == null
                ? !userRepository.existsByEmail(normalized)
                : !userRepository.existsByEmailAndIdNot(normalized, excludeId);
    }

    @Transactional(readOnly = true)
    public ProfileForm toProfileForm(Long userId) {
        User user = getById(userId);
        ProfileForm form = new ProfileForm();
        form.setId(user.getId());
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        form.setAddress(user.getAddress());
        return form;
    }

    // Cập nhật hồ sơ cá nhân; tự kiểm trùng email trừ chính mình.
    @Transactional
    public User updateProfile(Long userId, ProfileForm form) {
        User user = getById(userId);

        String normalizedEmail = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
            throw new BusinessRuleException("error.user.emailExists", normalizedEmail);
        }

        user.setFullName(form.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(form.getPhone());
        user.setAddress(form.getAddress());
        return user;
    }

    // Đổi mật khẩu: kiểm đúng mật khẩu hiện tại trước khi băm và lưu mật khẩu mới.
    @Transactional
    public void changePassword(Long userId, ChangePasswordForm form) {
        User user = getById(userId);

        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new BusinessRuleException("error.user.wrongCurrentPassword");
        }

        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        log.info("Người dùng {} đã đổi mật khẩu", user.getEmail());
    }

    @Transactional(readOnly = true)
    public Page<User> adminSearch(String keyword, Role role, int page) {
        return userRepository.search(keyword, role,
                PageRequest.of(Math.max(page, 0), ADMIN_PAGE_SIZE, Sort.by("fullName")));
    }

    // Admin tạo/sửa tài khoản: chặn tự hạ quyền/tự khoá mình và hạ quyền/khoá admin bật cuối cùng; đổi vai trò/trạng thái thì huỷ phiên đang đăng nhập của người đó.
    @Transactional
    public User saveFromAdmin(AdminUserForm form, Long currentUserId) {
        boolean creating = form.isNew();

        if (creating && (form.getNewPassword() == null || form.getNewPassword().isBlank())) {
            throw new BusinessRuleException("error.user.passwordRequired");
        }

        User user = creating ? new User() : getById(form.getId());
        boolean roleOrEnabledChanged = false;

        if (!creating) {
            boolean willDemoteOrLock = form.getRole() != Role.ADMIN || !form.isEnabled();

            if (willDemoteOrLock && user.getId().equals(currentUserId)) {
                throw new BusinessRuleException("error.user.edit.selfDemote");
            }

            if (user.isAdmin() && user.isEnabled() && willDemoteOrLock
                    && userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
                throw new BusinessRuleException("error.user.edit.lastAdmin");
            }

            roleOrEnabledChanged = user.getRole() != form.getRole() || user.isEnabled() != form.isEnabled();
        }

        user.setFullName(form.getFullName().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPhone(form.getPhone());
        user.setAddress(form.getAddress());
        user.setRole(form.getRole());
        user.setEnabled(form.isEnabled());

        if (form.getNewPassword() != null && !form.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        }

        User saved = userRepository.save(user);
        log.info("{} tài khoản {}", creating ? "Đã thêm" : "Đã cập nhật", saved.getEmail());

        if (roleOrEnabledChanged) {
            expireSessionsOf(saved.getId());
        }
        return saved;
    }

    // Xoá tài khoản: bảy quy tắc chặn (chính mình, admin cuối, còn đơn/đánh giá/lượt dùng mã/lịch sử đổi trạng thái/liên hệ đã xử lý).
    @Transactional
    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessRuleException("error.user.delete.self");
        }

        User user = getById(id);

        if (user.isAdmin() && userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new BusinessRuleException("error.user.delete.lastAdmin");
        }

        long bookingCount = bookingRepository.countByUserId(id);
        if (bookingCount > 0) {
            throw new BusinessRuleException("error.user.delete.hasBookings", bookingCount);
        }

        long reviewCount = reviewRepository.countByUserId(id);
        if (reviewCount > 0) {
            throw new BusinessRuleException("error.user.delete.hasReviews", reviewCount);
        }

        long couponCount = couponUsageRepository.countByUserId(id);
        if (couponCount > 0) {
            throw new BusinessRuleException("error.user.delete.hasCouponUsages", couponCount);
        }

        long historyCount = bookingStatusHistoryRepository.countByChangedById(id);
        if (historyCount > 0) {
            throw new BusinessRuleException("error.user.delete.hasBookingHistory", historyCount);
        }

        long contactCount = contactMessageRepository.countByHandledById(id);
        if (contactCount > 0) {
            throw new BusinessRuleException("error.user.delete.hasContactMessages", contactCount);
        }

        passwordResetTokenRepository.deleteByUserId(id);

        userRepository.delete(user);
        log.info("Đã xoá tài khoản {}", user.getEmail());
    }

    // Khoá/mở khoá tài khoản; chặn tự khoá mình và khoá admin bật cuối cùng, huỷ phiên nếu vừa khoá.
    @Transactional
    public boolean toggleEnabled(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessRuleException("error.user.disable.self");
        }

        User user = getById(id);

        if (user.isEnabled() && user.isAdmin()
                && userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new BusinessRuleException("error.user.delete.lastAdmin");
        }

        user.setEnabled(!user.isEnabled());

        if (!user.isEnabled()) {
            expireSessionsOf(user.getId());
        }
        return user.isEnabled();
    }

    // Huỷ ngay mọi phiên đăng nhập đang mở của một người dùng (đẩy về /login?expired ở lần request kế tiếp).
    private void expireSessionsOf(Long userId) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> principal instanceof CustomUserDetails cud
                        && cud.getId().equals(userId))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .forEach(SessionInformation::expireNow);
    }
}
