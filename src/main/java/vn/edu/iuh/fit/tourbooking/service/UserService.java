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

/**
 * Nghiệp vụ tài khoản người dùng, dùng chung cho cả người dùng tự thao tác lẫn
 * quản trị viên.
 */
@Service
@RequiredArgsConstructor
@Slf4j
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

    /**
     * Đăng ký tài khoản mới.
     *
     * <p>Vai trò luôn được gán cứng là {@link Role#CUSTOMER} tại đây chứ không
     * lấy từ biểu mẫu - đây là chốt chặn thứ hai sau việc {@code RegisterForm}
     * không hề có trường vai trò.</p>
     */
    @Transactional
    public User register(RegisterForm form) {
        // @UniqueEmail đã chặn ở tầng biểu mẫu; kiểm lại ở đây vì service còn
        // được gọi từ nơi khác (dữ liệu mẫu, kiểm thử) và vì giữa lúc kiểm tra
        // với lúc ghi vẫn có thể có người khác đăng ký cùng email.
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

        // Phát sự kiện thay vì gọi thẳng emailService ở đây: nếu giao dịch này bị
        // roll back (ví dụ ràng buộc khác ném lỗi sau dòng save), thư chào mừng
        // không được phép đã gửi đi cho một tài khoản chưa từng tồn tại. Nghe ở
        // UserEmailListener bằng @TransactionalEventListener(AFTER_COMMIT).
        eventPublisher.publishEvent(new UserRegisteredEvent(saved.getFullName(), saved.getEmail()));
        return saved;
    }

    /**
     * Sự kiện "đã đăng ký tài khoản", dùng để gửi thư chào mừng ngoài giao dịch.
     *
     * <p>Mang theo đúng hai chuỗi thay vì cả entity {@code User}: xem lý do đầy
     * đủ ở {@link vn.edu.iuh.fit.tourbooking.service.EmailService#sendWelcomeEmail}.</p>
     */
    public record UserRegisteredEvent(String fullName, String email) {
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("người dùng", id));
    }

    /**
     * Email này còn dùng được không - phục vụ web service kiểm tra ngay lúc gõ.
     *
     * <p>Chuẩn hoá y hệt lúc ghi ({@code trim} + hạ chữ thường), nếu không thì
     * {@code An.Nguyen@Gmail.com } báo là còn trống rồi tới lúc bấm Đăng ký mới
     * bị chặn - đúng kiểu lỗi làm người dùng bực nhất.</p>
     *
     * @param excludeId bỏ qua một tài khoản khi kiểm tra, dùng khi người dùng sửa
     *                  hồ sơ mà vẫn giữ nguyên email của chính mình
     */
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

    /** Đổ dữ liệu hiện tại vào biểu mẫu sửa hồ sơ. */
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

    /**
     * Cập nhật hồ sơ.
     *
     * <p>Nhận {@code userId} từ tài khoản đang đăng nhập, <b>không</b> dùng
     * {@code form.getId()}: nếu tin vào mã số gửi lên từ trình duyệt thì ai cũng
     * sửa được hồ sơ của người khác chỉ bằng cách đổi một con số.</p>
     */
    @Transactional
    public User updateProfile(Long userId, ProfileForm form) {
        User user = getById(userId);

        // Chốt chặn thứ hai cho tính duy nhất của email, độc lập với @UniqueEmail
        // ở tầng biểu mẫu. @UniqueEmail đọc excludeIdField NGAY LÚC BIND tham số
        // request - nếu id bị ai đó gửi kèm một giá trị khác userId (xem
        // AccountController @InitBinder chặn field "id"), phép kiểm ở biểu mẫu có
        // thể loại trừ nhầm bản ghi. Kiểm lại ở đây, luôn dùng userId thật của
        // phiên đăng nhập, để trường hợp xấu nhất vẫn dừng lại bằng một câu tiếng
        // Việt thay vì một lỗi UNIQUE 500 từ CSDL.
        String normalizedEmail = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
            throw new BusinessRuleException("error.user.emailExists", normalizedEmail);
        }

        user.setFullName(form.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(form.getPhone());
        user.setAddress(form.getAddress());
        return user;   // trong giao dịch nên Hibernate tự ghi lại khi commit
    }

    /**
     * Đổi mật khẩu.
     *
     * @throws BusinessRuleException nếu mật khẩu hiện tại không đúng
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordForm form) {
        User user = getById(userId);

        // matches() băm chuỗi người dùng nhập với chính muối của bản ghi cũ rồi
        // mới so sánh. Không bao giờ so sánh hai chuỗi băm với nhau: BCrypt sinh
        // muối ngẫu nhiên nên cùng một mật khẩu vẫn cho hai chuỗi băm khác nhau.
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new BusinessRuleException("error.user.wrongCurrentPassword");
        }

        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        log.info("Người dùng {} đã đổi mật khẩu", user.getEmail());
    }

    // =====================================================================
    //  Phần dành cho khu vực quản trị
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<User> adminSearch(String keyword, Role role, int page) {
        return userRepository.search(keyword, role,
                PageRequest.of(Math.max(page, 0), ADMIN_PAGE_SIZE, Sort.by("fullName")));
    }

    /**
     * Thêm mới hoặc cập nhật tài khoản từ khu vực quản trị.
     *
     * <p>Ô mật khẩu để trống khi sửa nghĩa là <b>giữ nguyên</b> mật khẩu cũ.
     * Biểu mẫu không bao giờ mang chuỗi băm cũ lên trình duyệt (xem
     * {@code AdminUserForm}), nên cũng không có gì để vô tình ghi đè bằng rác.</p>
     *
     * <p><b>{@code currentUserId} là tài khoản quản trị viên đang thao tác.</b>
     * Trước bản vá này, màn "sửa tài khoản" gán thẳng {@code role}/{@code enabled}
     * từ biểu mẫu mà không hề đi qua hai phép chặn vốn đã có sẵn ở {@link #delete}
     * và {@link #toggleEnabled} - một quản trị viên tự sửa hồ sơ của chính mình
     * thành {@code CUSTOMER} kèm bỏ tick "Kích hoạt" là tự khoá mình ra khỏi khu
     * vực quản trị ngay giữa lúc đang làm việc, hoặc hạ nốt quản trị viên đang bật
     * cuối cùng xuống thường thì không còn ai vào được {@code /admin} nữa. Chỉ áp
     * dụng khi <b>sửa</b> ({@code !creating}); tạo mới không thể tự nhắm vào chính
     * mình vì tài khoản chưa tồn tại.</p>
     */
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

            // Đang là admin bật, và biểu mẫu định hạ quyền hoặc khoá -> phải chắc
            // chắn còn ít nhất một quản trị viên khác đang bật sau thao tác này.
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

        // 1.8: vai trò hoặc trạng thái bật/tắt vừa đổi - phiên đăng nhập cũ của
        // người này (nếu có) đang cầm một CustomUserDetails với vai trò/trạng thái
        // CŨ trong SecurityContext, sẽ còn hiệu lực cho tới khi phiên tự hết hạn
        // nếu không chủ động đánh dấu expired ngay tại đây.
        if (roleOrEnabledChanged) {
            expireSessionsOf(saved.getId());
        }
        return saved;
    }

    /**
     * Xoá một tài khoản.
     *
     * <p>Quy tắc chặn xoá thứ tư của đề bài, và là quy tắc nhiều điều kiện nhất -
     * cả ba đều kiểm tra bằng Java:</p>
     * <ol>
     *   <li><b>Không tự xoá chính mình</b> - tự khoá mình ra khỏi hệ thống ngay
     *       giữa lúc đang làm việc.</li>
     *   <li><b>Không xoá quản trị viên cuối cùng</b> - xoá xong thì không còn ai
     *       vào được khu vực quản trị, phải sửa thẳng trong CSDL mới gỡ được.</li>
     *   <li><b>Không xoá tài khoản đã từng đặt tour</b> - đơn hàng là chứng từ,
     *       mất người đặt là mất dấu vết giao dịch.</li>
     * </ol>
     */
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

        // Bốn phép kiểm dưới đây sinh ra cùng lúc với bốn bảng bổ sung. Không có
        // chúng thì khoá ngoại vẫn chặn được, nhưng người dùng chỉ nhận một lỗi
        // SQL khó hiểu kèm trang 500 - đúng thứ mà đề bài muốn tránh.
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

        // Vé đặt lại mật khẩu là thứ DUY NHẤT bị xoá theo người dùng thay vì chặn:
        // nó không có giá trị lưu trữ, giữ lại chỉ tổ thành chìa khoá bỏ quên.
        passwordResetTokenRepository.deleteByUserId(id);

        userRepository.delete(user);
        log.info("Đã xoá tài khoản {}", user.getEmail());
    }

    /**
     * Vô hiệu hoá / kích hoạt lại tài khoản - phương án thay thế khi không xoá được.
     *
     * <p>Vẫn phải chặn hai trường hợp tự khoá mình và khoá quản trị viên cuối
     * cùng: hậu quả giống hệt như xoá.</p>
     */
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

        // 1.8: chỉ cần đá phiên khi vừa KHOÁ (chuyển sang !enabled) - mở lại tài
        // khoản không phải một tình huống cần buộc đăng xuất khỏi đâu cả, và
        // người bị khoá lúc trước gần như chắc chắn đã bị đá ra từ lần khoá đó rồi.
        if (!user.isEnabled()) {
            expireSessionsOf(user.getId());
        }
        return user.isEnabled();
    }

    /**
     * Đánh dấu mọi phiên đang mở của một người dùng là {@code expired} trong
     * {@link SessionRegistry} (1.8).
     *
     * <p>{@code ConcurrentSessionFilter} (đăng ký tự động nhờ khai báo
     * {@code sessionConcurrency} ở {@code SecurityConfig}) kiểm tra cờ này ở MỖI
     * request tiếp theo của phiên đó - khớp thì tự invalidate phiên và đưa người
     * dùng về {@code /login?expired}. Không đụng gì tới phiên của người khác:
     * {@link CustomUserDetails#getId()} là khoá so khớp duy nhất.</p>
     */
    private void expireSessionsOf(Long userId) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> principal instanceof CustomUserDetails cud
                        && cud.getId().equals(userId))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .forEach(SessionInformation::expireNow);
    }
}
