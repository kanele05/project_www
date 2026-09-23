package vn.edu.iuh.fit.tourbooking.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.config.AppProperties;
import vn.edu.iuh.fit.tourbooking.dto.form.CheckoutForm;
import vn.edu.iuh.fit.tourbooking.dto.form.PassengerForm;
import vn.edu.iuh.fit.tourbooking.dto.form.PassengerGroupForm;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.BookingDetail;
import vn.edu.iuh.fit.tourbooking.entity.BookingPassenger;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatusHistory;
import vn.edu.iuh.fit.tourbooking.entity.PassengerType;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.tourbooking.repository.BookingPassengerRepository;
import vn.edu.iuh.fit.tourbooking.repository.BookingRepository;
import vn.edu.iuh.fit.tourbooking.repository.BookingStatusHistoryRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourDepartureRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserRepository;
import vn.edu.iuh.fit.tourbooking.session.Cart;
import vn.edu.iuh.fit.tourbooking.session.CartItem;
import vn.edu.iuh.fit.tourbooking.util.CodeGenerator;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Nghiệp vụ đặt tour.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    /** Số đơn hiển thị trên một trang ở mục "Đơn của tôi". */
    public static final int PAGE_SIZE = 10;

    /** Bảng danh sách đơn ở khu vực quản trị. */
    public static final int ADMIN_PAGE_SIZE = 15;

    private final BookingRepository bookingRepository;
    private final TourDepartureRepository departureRepository;
    private final UserRepository userRepository;
    private final PromotionService promotionService;
    private final PaymentService paymentService;
    private final BookingStatusHistoryRepository bookingStatusHistoryRepository;
    private final BookingPassengerRepository bookingPassengerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AppProperties appProperties;
    private final MessageHelper messages;
    private final EntityManager entityManager;

    /** Độ dài tối đa hợp lý cho một họ tên hành khách - khớp {@code booking_passengers.full_name NVARCHAR(100)}. */
    private static final int MAX_PASSENGER_NAME_LENGTH = 100;

    /**
     * Khoá đơn theo mã tra cứu <b>TRƯỚC</b> khi đổi trạng thái/số khách (mục
     * "NGHIÊM TRỌNG - 1", đã tái hiện bằng thao tác thật hai kịch bản: mất khoản
     * đã thu khi khách tự huỷ trùng lúc admin đánh dấu đã thu tiền, và lách máy
     * trạng thái khi admin xác nhận trùng lúc khách tự huỷ). Đây là dòng ĐẦU TIÊN
     * của {@link #updateStatus}, {@link #cancelBySelf}, {@link #expirePendingBooking}
     * và {@link #updateDetailQuantity} - bên thua phải đợi bên thắng commit xong
     * rồi mới đọc lại đúng trạng thái mới nhất và bị chặn bằng
     * {@link BusinessRuleException} (thông qua các phép kiểm trạng thái đã có sẵn
     * ngay sau lời gọi này), không ghi đè lên nhau nữa.
     *
     * <p>Gotcha #60: chỉ khoá ở tầng CSDL thôi CHƯA ĐỦ -
     * {@code entityManager.refresh(...)} bắt buộc ngay sau khi giữ được khoá, để
     * không đọc nhầm bản entity cũ đã nằm sẵn trong cache cấp một của cùng giao
     * dịch (giống hệt cách {@code PromotionService.recordUsage} đã làm với
     * {@code Promotion}).</p>
     */
    private Booking lockBookingByCode(String code) {
        Booking locked = bookingRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> ResourceNotFoundException.of("đơn đặt tour", code));
        entityManager.refresh(locked);
        return locked;
    }

    /** Cùng {@link #lockBookingByCode} nhưng theo khoá chính, trả {@code null} nếu không còn tồn tại. */
    private Booking lockBookingById(Long id) {
        Booking locked = bookingRepository.findByIdForUpdate(id).orElse(null);
        if (locked == null) {
            return null;
        }
        entityManager.refresh(locked);
        return locked;
    }

    /**
     * Chốt đơn đặt tour: <b>toàn bộ nằm trong một giao dịch duy nhất</b>.
     *
     * <p>Hoặc là mọi thứ cùng thành công - đơn được ghi, số chỗ bị trừ - hoặc là
     * không có gì xảy ra cả. Nếu tách thành nhiều giao dịch, chỉ cần bước trừ chỗ
     * hỏng là hệ thống có một đơn hàng "ma" không giữ chỗ nào.</p>
     *
     * <p><b>Giá được đọc lại từ CSDL</b> qua {@code new BookingDetail(departure, ...)}
     * chứ không lấy giá đang nằm trong giỏ hàng ở session. Giỏ hàng nằm phía người
     * dùng nên về nguyên tắc là không đáng tin; hơn nữa quản trị viên có thể vừa
     * chỉnh giá trong lúc khách còn để tour trong giỏ.</p>
     *
     * <p>Giỏ hàng <b>không</b> bị xoá ở đây mà do controller xoá sau khi phương
     * thức này trả về thành công. Xoá ngay tại đây thì lỡ giao dịch không commit
     * được (ví dụ hai khách tranh nhau chỗ cuối), khách vừa mất giỏ hàng vừa
     * không có đơn nào.</p>
     *
     * @return đơn hàng vừa tạo, đã có mã tra cứu
     */
    @Transactional
    public Booking placeOrder(Long userId, CheckoutForm form, Cart cart) {
        if (cart == null || cart.isEmpty()) {
            throw new BusinessRuleException("error.checkout.emptyCart");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("người dùng", userId));

        Booking booking = new Booking();
        booking.setCode(CodeGenerator.uniqueBookingCode(bookingRepository::existsByCode));
        booking.setUser(user);
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);

        // Chép thông tin liên hệ vào đơn. Từ giờ đơn này không còn phụ thuộc vào
        // bảng users nữa: khách có đổi số điện thoại thì đơn cũ vẫn giữ số cũ.
        booking.setCustomerName(form.getCustomerName().trim());
        booking.setCustomerEmail(form.getCustomerEmail().trim());
        booking.setCustomerPhone(form.getCustomerPhone().trim());
        booking.setCustomerAddress(form.getCustomerAddress());
        booking.setPaymentMethod(form.getPaymentMethod());
        booking.setNote(form.getNote());

        for (CartItem item : cart.getItems()) {
            // Khoá bi quan: hai khách cùng giành chỗ cuối cùng thì người thứ hai
            // phải chờ, đọc lại số chỗ đã cập nhật rồi mới quyết định. Không có
            // khoá này thì cả hai cùng đọc "còn 1 chỗ" và cùng đặt thành công.
            TourDeparture departure = departureRepository.findByIdForUpdate(item.getDepartureId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "error.checkout.departureGone", item.getTourName()));

            // isBookable(cutoffDays) chỉ xét chính đợt khởi hành (mục 12.5: hạn
            // chót đặt tour); tour "Ngừng bán" vẫn có đợt active còn chỗ nên phải
            // kiểm riêng tour.isActive() ở đây. departure được nạp bằng
            // findByIdForUpdate (không JOIN FETCH tour), nhưng đang ở trong giao
            // dịch @Transactional của phương thức này nên chạm vào quan hệ LAZY
            // departure.getTour() vẫn an toàn (khác hẳn open-in-view=false ở tầng
            // template).
            if (!departure.isBookable(appProperties.booking().cutoffDays()) || !departure.getTour().isActive()) {
                throw new BusinessRuleException("error.checkout.notBookable", item.getTourName());
            }

            int seats = item.getQuantity();
            if (!departure.hasEnoughSeats(seats)) {
                throw new BusinessRuleException("error.checkout.notEnoughSeats",
                        item.getTourName(), departure.getAvailableSeats());
            }

            departure.holdSeats(seats);
            BookingDetail detail = new BookingDetail(departure, item.getNumAdults(), item.getNumChildren());

            // Mục 12.7: gắn danh sách hành khách của đúng dòng giỏ hàng này. Đây
            // là lần kiểm THỨ HAI (lần đầu ở CheckoutController.validatePassengerStructure)
            // - không tin cấu trúc form đã qua được tầng controller, vì giữa lúc
            // khách gửi biểu mẫu và lúc giao dịch này chạy, giỏ hàng có thể đã đổi
            // (ví dụ mở hai tab). Sai gì cũng huỷ toàn bộ giao dịch, không ghi dở dang.
            attachPassengers(detail, findGroup(form, item.getDepartureId()), item);

            booking.addDetail(detail);
        }

        // Mã giảm giá: kiểm LẠI ở đây chứ không tin kết quả của nút "Áp dụng" bên
        // trang thanh toán. Giữa lúc khách bấm xem trước và lúc bấm đặt, mã có thể
        // đã hết lượt - và biểu mẫu hoàn toàn có thể được gửi thẳng không qua nút đó.
        // Số tiền giảm luôn tính lại từ CSDL, biểu mẫu chỉ mang mỗi cái mã.
        PromotionService.CouponCheck coupon = null;
        if (form.getCouponCode() != null && !form.getCouponCode().isBlank()) {
            coupon = promotionService.check(form.getCouponCode(), userId, booking.getSubtotalAmount());
            booking.setPromotion(coupon.promotion());
            booking.setDiscountAmount(coupon.discount());
        }

        booking.recalculateTotal();
        Booking saved = bookingRepository.save(booking);

        if (coupon != null) {
            // Nằm trong cùng giao dịch: đơn hỏng thì lượt dùng mã cũng biến mất.
            promotionService.recordUsage(coupon.promotion(), user, saved, coupon.discount());
        }

        // Bổ sung A: dòng đầu tiên của nhật ký trạng thái - fromStatus = null vì
        // đơn vừa được tạo, chưa có trạng thái cũ nào. changedBy là chính khách
        // hàng, không phải hệ thống: đặt tour là một thao tác có người đứng sau.
        bookingStatusHistoryRepository.save(
                new BookingStatusHistory(saved, null, BookingStatus.PENDING, user, null));

        // Bổ sung B: sinh sẵn một khoản phải thu PENDING - cùng giao dịch với
        // đơn, đơn hỏng thì khoản thu ma cũng biến mất theo.
        paymentService.createPendingForBooking(saved);

        // Chỉ phát tín hiệu ở đây; thư được gửi SAU KHI giao dịch commit
        // (xem BookingEmailListener). Gửi ngay tại đây thì lỡ giao dịch bị huỷ,
        // khách đã cầm trong tay thư xác nhận một đơn hàng không tồn tại.
        eventPublisher.publishEvent(new BookingPlacedEvent(saved.getCode()));

        log.info("Đã tạo đơn {} cho {} - {} dòng, tổng {} đ",
                saved.getCode(), user.getEmail(), saved.getDetails().size(), saved.getTotalAmount());
        return saved;
    }

    /** Tìm đúng nhóm hành khách của một dòng giỏ hàng theo {@code departureId}. */
    private PassengerGroupForm findGroup(CheckoutForm form, Long departureId) {
        if (form.getPassengerGroups() == null) {
            return null;
        }
        return form.getPassengerGroups().stream()
                .filter(g -> departureId.equals(g.getDepartureId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gắn danh sách hành khách vào một dòng chi tiết vừa tạo (mục 12.7).
     *
     * <p><b>Không tin dữ liệu gửi lên</b>: số hành khách theo từng loại phải
     * khớp CHÍNH XÁC {@code numAdults}/{@code numChildren} của dòng - lệch một
     * người cũng bị chặn (kể cả khi tầng controller đã kiểm - phòng khi giỏ hàng
     * đổi giữa hai lần kiểm, hoặc yêu cầu được gửi thẳng không qua controller
     * này, ví dụ trong kiểm thử). Mỗi tên đều được kiểm lại {@code NotBlank} và
     * ngày sinh không ở tương lai - <b>không tin @Valid ở tầng trước đã chặn
     * hết</b>.</p>
     */
    private void attachPassengers(BookingDetail detail, PassengerGroupForm group, CartItem item) {
        List<PassengerForm> adults = group == null || group.getAdults() == null
                ? List.of() : group.getAdults();
        List<PassengerForm> children = group == null || group.getChildren() == null
                ? List.of() : group.getChildren();

        if (adults.size() != item.getNumAdults() || children.size() != item.getNumChildren()) {
            throw new BusinessRuleException("error.checkout.passengerMismatch");
        }

        for (PassengerForm p : adults) {
            detail.getPassengers().add(toPassenger(detail, p, PassengerType.ADULT));
        }
        for (PassengerForm p : children) {
            detail.getPassengers().add(toPassenger(detail, p, PassengerType.CHILD));
        }
    }

    private BookingPassenger toPassenger(BookingDetail detail, PassengerForm form, PassengerType type) {
        if (form.getFullName() == null || form.getFullName().isBlank()) {
            throw new BusinessRuleException("error.checkout.passengerNameRequired");
        }
        if (form.getBirthDate() != null && form.getBirthDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("error.checkout.passengerBirthDateFuture");
        }
        BookingPassenger passenger = new BookingPassenger(form.getFullName().trim(), type);
        passenger.setDetail(detail);
        passenger.setGender(form.getGender());
        passenger.setBirthDate(form.getBirthDate());
        passenger.setIdNumber(blankToNull(form.getIdNumber()));
        passenger.setPhone(blankToNull(form.getPhone()));
        passenger.setSingleRoom(form.isSingleRoom());
        passenger.setNote(blankToNull(form.getNote()));
        return passenger;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Lịch sử đặt tour của một khách hàng, mới nhất lên đầu. */
    @Transactional(readOnly = true)
    public Page<Booking> findByUser(Long userId, int page) {
        return bookingRepository.findByUserIdOrderByBookingDateDesc(
                userId, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    /**
     * Nạp một đơn theo mã tra cứu, <b>kèm kiểm tra quyền sở hữu</b>.
     *
     * <p>Địa chỉ dùng mã đơn thay cho khoá chính tuần tự đã khiến việc đoán mò
     * khó hơn nhiều, nhưng khó đoán không phải là bảo mật. Phép so sánh chủ sở
     * hữu bên dưới mới là thứ thực sự chặn được người khác xem đơn của mình.</p>
     *
     * @param requesterId  tài khoản đang đăng nhập
     * @param requesterIsAdmin quản trị viên được xem mọi đơn
     */
    @Transactional(readOnly = true)
    public Booking getOwnedByCode(String code, Long requesterId, boolean requesterIsAdmin) {
        Booking booking = bookingRepository.findDetailByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("đơn đặt tour", code));

        if (!requesterIsAdmin && !booking.getUser().getId().equals(requesterId)) {
            log.warn("Tài khoản id={} cố xem đơn {} của người khác", requesterId, code);
            throw new org.springframework.security.access.AccessDeniedException(
                    "Không có quyền xem đơn này");
        }
        return booking;
    }

    /** Nạp đơn kèm chi tiết để dựng nội dung thư - dùng sau khi giao dịch đã commit. */
    @Transactional(readOnly = true)
    public Booking getDetailByCode(String code) {
        return bookingRepository.findDetailByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("đơn đặt tour", code));
    }

    // =====================================================================
    //  Phần dành cho khu vực quản trị
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<Booking> adminSearch(String keyword, BookingStatus status, int page) {
        return bookingRepository.adminSearch(keyword, status,
                // Mặc định sắp xếp đơn mới nhất lên đầu - người trực đơn quan tâm
                // đơn vừa vào, không phải đơn từ năm ngoái.
                PageRequest.of(Math.max(page, 0), ADMIN_PAGE_SIZE,
                        Sort.by(Sort.Direction.DESC, "bookingDate")));
    }

    /**
     * Đổi trạng thái đơn ở màn quản trị (mục 12.1: PENDING&rarr;CONFIRMED/CANCELLED,
     * CONFIRMED&rarr;COMPLETED/CANCELLED; COMPLETED và CANCELLED là trạng thái
     * <b>cuối</b>, không có nhánh "khôi phục đơn đã huỷ" nữa).
     *
     * <p>Đây là phần dễ sai nhất của màn quản trị đơn hàng: huỷ đơn mà quên trả
     * chỗ thì những chỗ đó "bốc hơi" - không ai đặt được nhưng cũng không ai đi.
     * Sang {@code COMPLETED} chỉ được chấp nhận khi ngày khởi hành đã tới (hoặc
     * qua) <b>và</b> đơn đã có khoản ĐÃ THANH TOÁN - xem {@link #isCompletable}.</p>
     *
     * <p>Bổ sung A: mỗi lần đổi đều ghi thêm một dòng {@code booking_status_history}
     * - đây là chỗ duy nhất trong ứng dụng cập nhật trạng thái đơn <i>sau khi đặt</i>
     * mà không phải là khách tự huỷ ({@link #cancelBySelf}) hay hệ thống tự huỷ
     * quá hạn ({@link #expirePendingBooking}).</p>
     *
     * @param changedByUserId quản trị viên đang thao tác
     * @param reason          ghi chú của quản trị viên, tuỳ chọn
     */
    @Transactional
    public Booking updateStatus(String code, BookingStatus newStatus, Long changedByUserId, String reason) {
        // Khoá đơn TRƯỚC KHI đọc trạng thái - xem Javadoc lockBookingByCode.
        Booking booking = lockBookingByCode(code);

        BookingStatus oldStatus = booking.getStatus();
        if (oldStatus == newStatus) {
            return booking;
        }
        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new BusinessRuleException("error.booking.invalidTransition",
                    messages.get(oldStatus.getMessageKey()), messages.get(newStatus.getMessageKey()));
        }

        if (newStatus == BookingStatus.COMPLETED) {
            requireCompletable(booking);
        }
        if (newStatus == BookingStatus.CANCELLED) {
            // Huỷ đơn: trả chỗ về cho các đợt khởi hành, trả lượt dùng mã (nếu có).
            releaseHold(booking);
            // Bổ sung B: đóng luôn khoản thu PENDING - không thì nút "Đã thu tiền"
            // của một đơn đã huỷ vẫn bấm được (xem PaymentService.cancelPendingForBooking).
            paymentService.cancelPendingForBooking(booking);
            // Mục 12.2: quản trị viên huỷ luôn hoàn 100% khoản đã thu (lỗi phía
            // công ty) - không đổi schema, chỉ THÊM một dòng REFUNDED mới.
            paymentService.refundIfPaid(booking, 100, messages.get("payment.note.refund.adminCancel"));
        }

        booking.setStatus(newStatus);

        // getReferenceById thay vì findById: chỉ cần một proxy đủ để gắn khoá
        // ngoại, không cần nạp cả bản ghi User cho một cột chỉ dùng để tham chiếu.
        User changedBy = changedByUserId == null ? null : userRepository.getReferenceById(changedByUserId);
        bookingStatusHistoryRepository.save(
                new BookingStatusHistory(booking, oldStatus, newStatus, changedBy,
                        (reason == null || reason.isBlank()) ? null : reason.trim()));

        log.info("Đơn {}: {} -> {}", code, oldStatus, newStatus);
        return booking;
    }

    /**
     * Các trạng thái đích hợp lệ cho đơn đang xem - dùng để dựng ô chọn trạng
     * thái ở màn quản trị (mục 12.1: "Giao diện admin chỉ liệt kê các trạng thái
     * đích hợp lệ"). Loại {@code COMPLETED} khỏi danh sách nếu đơn chưa đủ điều
     * kiện ({@link #isCompletable}) - không có lý do gì cho quản trị viên chọn
     * một lựa chọn chắc chắn sẽ bị từ chối.
     */
    @Transactional(readOnly = true)
    public List<BookingStatus> validNextStatuses(Booking booking) {
        List<BookingStatus> result = new ArrayList<>();
        for (BookingStatus target : BookingStatus.values()) {
            if (!booking.getStatus().canTransitionTo(target)) {
                continue;
            }
            if (target == BookingStatus.COMPLETED && !isCompletable(booking)) {
                continue;
            }
            result.add(target);
        }
        return result;
    }

    /** Điều kiện phụ của mục 12.1 để một đơn được sang HOÀN TẤT. */
    private boolean isCompletable(Booking booking) {
        return !departureNotYetReached(booking) && paymentService.hasPaidPayment(booking.getId());
    }

    private void requireCompletable(Booking booking) {
        if (departureNotYetReached(booking)) {
            throw new BusinessRuleException("error.booking.complete.notYetDeparted");
        }
        if (!paymentService.hasPaidPayment(booking.getId())) {
            throw new BusinessRuleException("error.booking.complete.notPaid");
        }
    }

    /** true nếu còn ít nhất một dòng của đơn có ngày khởi hành sau hôm nay. */
    private boolean departureNotYetReached(Booking booking) {
        return booking.getDetails().stream()
                .map(d -> d.getDeparture().getDepartureDate())
                .anyMatch(date -> date.isAfter(LocalDate.now()));
    }

    /** Trả chỗ cho các đợt khởi hành và trả lượt dùng mã giảm giá (nếu có) khi huỷ đơn. */
    private void releaseHold(Booking booking) {
        booking.getDetails().forEach(d -> d.getDeparture().releaseSeats(d.getTotalGuests()));
        promotionService.releaseUsage(booking);
    }

    // =====================================================================
    //  UC023 - Khách tự huỷ đơn (mục 12.3)
    // =====================================================================

    /**
     * Kết quả tính chính sách tự huỷ cho một đơn, dùng cả để hiển thị hộp xác
     * nhận ("số tiền sẽ được hoàn") lẫn để {@link #cancelBySelf} kiểm lại trước
     * khi thực sự huỷ - không tin con số đã hiện trên trang GET trước đó.
     *
     * @param timingOk         còn đủ ngày để tự huỷ theo {@code app.booking.self-cancel-min-days}
     * @param refundPercent    tỉ lệ hoàn nếu huỷ ngay bây giờ (100 hoặc {@code partial-refund-percent})
     * @param refundAmount     số tiền sẽ hoàn nếu huỷ ngay bây giờ (0 nếu {@code !timingOk})
     * @param daysUntilDeparture số ngày còn lại tới đợt khởi hành GẦN NHẤT trong đơn
     */
    public record SelfCancelPolicy(boolean timingOk, int refundPercent,
                                   BigDecimal refundAmount, long daysUntilDeparture) {
    }

    /**
     * Tính chính sách tự huỷ theo mục 12.3 dựa trên đợt khởi hành <b>gần nhất</b>
     * trong đơn (an toàn nhất khi đơn gộp nhiều tour: chưa chắc huỷ được nếu bất
     * kỳ tour nào trong đơn đã cận ngày).
     */
    @Transactional(readOnly = true)
    public SelfCancelPolicy evaluateSelfCancel(Booking booking) {
        AppProperties.Booking cfg = appProperties.booking();

        long daysUntil = booking.getDetails().stream()
                .map(d -> d.getDeparture().getDepartureDate())
                .min(Comparator.naturalOrder())
                .map(date -> ChronoUnit.DAYS.between(LocalDate.now(), date))
                .orElse(0L);

        boolean timingOk = daysUntil >= cfg.selfCancelMinDays();
        int refundPercent = daysUntil >= cfg.fullRefundMinDays() ? 100 : cfg.partialRefundPercent();
        BigDecimal refundAmount = BigDecimal.ZERO;
        if (timingOk) {
            BigDecimal paidTotal = paymentService.paidTotal(booking.getId());
            refundAmount = paidTotal
                    .multiply(BigDecimal.valueOf(refundPercent))
                    .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
        }
        return new SelfCancelPolicy(timingOk, refundPercent, refundAmount, daysUntil);
    }

    /**
     * Khách tự huỷ đơn của chính mình (UC023).
     *
     * <p>Mọi điều kiện được kiểm LẠI ở đây, không tin trang GET đã hiện gì trước
     * đó - cùng nguyên tắc với {@code PromotionService.check} rồi {@code recordUsage}:
     * giữa lúc khách mở trang và lúc bấm huỷ, tình huống có thể đã đổi.</p>
     *
     * @throws AccessDeniedException nếu không phải đơn của chính khách
     * @throws BusinessRuleException nếu đơn không còn huỷ được (đã ở trạng thái
     *                                cuối) hoặc đã quá cận ngày khởi hành
     */
    @Transactional
    public Booking cancelBySelf(String code, Long userId) {
        // Khoá đơn TRƯỚC KHI đọc trạng thái - xem Javadoc lockBookingByCode. Đây
        // chính là vế "khách tự huỷ" của kịch bản A/B đã tái hiện bằng thao tác
        // thật: nếu thua cuộc đua với admin (updateStatus/markPaid), isCancellable()
        // bên dưới sẽ đọc đúng trạng thái mới nhất và chặn lại bằng BusinessRuleException.
        Booking booking = lockBookingByCode(code);

        if (!booking.getUser().getId().equals(userId)) {
            log.warn("Tài khoản id={} cố tự huỷ đơn {} của người khác", userId, code);
            throw new AccessDeniedException("Không có quyền huỷ đơn này");
        }
        if (!booking.isCancellable()) {
            throw new BusinessRuleException("error.booking.selfCancel.notCancellable");
        }

        SelfCancelPolicy policy = evaluateSelfCancel(booking);
        if (!policy.timingOk()) {
            throw new BusinessRuleException("error.booking.selfCancel.tooLate",
                    appProperties.booking().selfCancelMinDays());
        }

        BookingStatus oldStatus = booking.getStatus();
        releaseHold(booking);
        paymentService.cancelPendingForBooking(booking);
        paymentService.refundIfPaid(booking, policy.refundPercent(),
                messages.get("payment.note.refund.selfCancel", policy.refundPercent()));

        booking.setStatus(BookingStatus.CANCELLED);
        bookingStatusHistoryRepository.save(new BookingStatusHistory(booking, oldStatus,
                BookingStatus.CANCELLED, userRepository.getReferenceById(userId), "Khách tự huỷ"));

        log.info("Đơn {}: khách tự huỷ, hoàn {}% ({} đ)",
                code, policy.refundPercent(), policy.refundAmount());
        return booking;
    }

    // =====================================================================
    //  Mục 12.4 - hệ thống tự huỷ đơn CHỜ chưa thanh toán quá hạn
    // =====================================================================

    /**
     * Huỷ MỘT đơn CHỜ XÁC NHẬN đã quá hạn thanh toán - gọi từ
     * {@code BookingExpiryScheduler}, <b>mỗi đơn một giao dịch riêng</b> (yêu cầu
     * của mục 12.4) để một đơn lỗi không kéo cả mẻ đang xử lý.
     *
     * <p>Nạp lại và kiểm tra trạng thái NGAY TẠI ĐÂY (không tin danh sách id đã
     * truy vấn trước đó ở tầng lập lịch): giữa lúc liệt kê và lúc xử lý, đơn có
     * thể đã được quản trị viên xác nhận hoặc khách đã thanh toán - bỏ qua chứ
     * không huỷ nhầm.</p>
     */
    @Transactional
    public void expirePendingBooking(Long bookingId) {
        // Khoá đơn TRƯỚC KHI đọc trạng thái - xem Javadoc lockBookingByCode.
        Booking booking = lockBookingById(bookingId);
        if (booking == null || booking.getStatus() != BookingStatus.PENDING) {
            return;
        }
        if (paymentService.hasPaidPayment(booking.getId())) {
            return;
        }

        BookingStatus oldStatus = booking.getStatus();
        releaseHold(booking);
        paymentService.cancelPendingForBooking(booking);

        booking.setStatus(BookingStatus.CANCELLED);
        // changedBy = null: hệ thống tự đổi, không có quản trị viên nào đứng sau
        // (mục 12.4).
        bookingStatusHistoryRepository.save(new BookingStatusHistory(booking, oldStatus,
                BookingStatus.CANCELLED, null, "Quá hạn thanh toán"));

        log.info("Đơn {} tự huỷ do quá hạn thanh toán (quá {} giờ)",
                booking.getCode(), appProperties.booking().pendingExpiryHours());
    }

    /**
     * Kết quả một lần gọi {@link #updateDetailQuantity}: hoặc áp dụng luôn, hoặc
     * còn thiếu tên cho một số hành khách <b>mới</b> - khi đó phương thức
     * <b>chưa đổi gì cả</b> (kể cả số chỗ), gọi lại đúng nó kèm đủ tên
     * ({@code newAdultNames}/{@code newChildNames}) là xong (mục 12.7).
     */
    public record PassengerNameGap(int adultsNeeded, int childrenNeeded, List<String> droppedNames) {
        public boolean isEmpty() {
            return adultsNeeded <= 0 && childrenNeeded <= 0;
        }
    }

    /**
     * Sửa số khách của một dòng trong đơn - giao diện tối thiểu cho quản trị
     * viên giữ đúng bất biến "số hành khách theo loại luôn khớp
     * {@code numAdults}/{@code numChildren}" (mục 12.7), kể cả khi đề bài bắt
     * buộc chức năng sửa số lượng này phải còn dùng được:
     *
     * <ul>
     *   <li><b>Tăng</b> số khách một loại &rArr; phải có đủ {@code newAdultNames}/
     *       {@code newChildNames} cho đúng số người MỚI; thiếu tên thì
     *       {@link PassengerNameGap#isEmpty()} trả {@code false} và <b>không có
     *       gì được ghi</b> - controller hiện lại đúng bấy nhiêu ô nhập tên.</li>
     *   <li><b>Giảm</b> số khách một loại &rArr; tự động bỏ bớt hành khách có id
     *       LỚN NHẤT của đúng loại đó (người được thêm gần đây nhất) - đơn giản
     *       hơn hẳn so với để quản trị viên tự chọn từng người mà vẫn đúng.</li>
     * </ul>
     *
     * <p>Số lượng hành khách HIỆN CÓ được đếm trực tiếp trong CSDL (không dựa
     * vào {@code detail.numAdults} cũ) nên phương thức này còn <b>tự chữa</b>
     * được cả những dòng đã lệch bất biến từ trước (ví dụ đặt qua bản chưa có
     * màn nhập hành khách): gọi lại với đúng số khách hiện tại sẽ được yêu cầu
     * bổ sung đủ tên còn thiếu.</p>
     *
     * <p>Số chỗ được điều chỉnh theo <b>mức chênh lệch</b>, và tổng tiền của dòng
     * lẫn của cả đơn đều được tính lại - ba con số đó phải luôn khớp nhau, nếu
     * không thì báo cáo doanh thu và số chỗ còn trống đều sai.</p>
     */
    @Transactional
    public PassengerNameGap updateDetailQuantity(String code, Long detailId, int adults, int children,
                                                  List<String> newAdultNames, List<String> newChildNames) {
        // Khoá đơn TRƯỚC KHI đọc trạng thái - xem Javadoc lockBookingByCode.
        Booking booking = lockBookingByCode(code);

        if (booking.getStatus().isFinal()) {
            throw new BusinessRuleException("error.booking.finalStatus");
        }
        // Đơn đã có khoản thu PAID: chặn sửa số khách thay vì âm thầm đổi tổng
        // tiền của một đơn đã thu tiền thật. Luồng hoàn tiền/thu thêm đúng nghĩa
        // (hoàn một phần, thu bù chênh lệch...) là quyết định nghiệp vụ đang chờ
        // quyết định của người phụ trách - tạm thời cách an toàn nhất là chặn hẳn
        // và gợi ý liên hệ bộ phận kế toán để xử lý thủ công.
        if (paymentService.hasPaidPayment(booking.getId())) {
            throw new BusinessRuleException("error.booking.detailQuantity.hasPaidPayment");
        }
        if (adults < 1) {
            throw new BusinessRuleException("error.cart.needAdult");
        }
        // Gửi thẳng numChildren âm (bỏ qua ràng buộc min="0" của HTML) từng khiến
        // delta ra âm, releaseSeats() trả lại chỗ CHƯA TỪNG giữ (bán quá chỗ) và
        // recalculateSubtotal() ra tiền âm. Trần tổng khách dùng lại đúng hằng số
        // của giỏ hàng (CartService.MAX_GUESTS_PER_ITEM) - không có lý do gì hai
        // nơi có hai trần khác nhau.
        if (children < 0) {
            throw new BusinessRuleException("error.cart.invalidChildren");
        }
        if (adults + children > CartService.MAX_GUESTS_PER_ITEM) {
            throw new BusinessRuleException("error.cart.tooManyGuests", CartService.MAX_GUESTS_PER_ITEM);
        }

        BookingDetail detail = booking.getDetails().stream()
                .filter(d -> d.getId().equals(detailId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("dòng chi tiết", detailId));

        long currentAdultPassengers = bookingPassengerRepository
                .countByDetailIdAndPassengerType(detailId, PassengerType.ADULT);
        long currentChildPassengers = bookingPassengerRepository
                .countByDetailIdAndPassengerType(detailId, PassengerType.CHILD);
        int adultGap = (int) (adults - currentAdultPassengers);
        int childGap = (int) (children - currentChildPassengers);

        List<String> cleanAdultNames = cleanNames(newAdultNames);
        List<String> cleanChildNames = cleanNames(newChildNames);

        // Tăng mà chưa đủ tên: KHÔNG áp dụng gì cả (kể cả số chỗ/thành tiền) -
        // trả phần còn thiếu để controller hiện lại đúng bấy nhiêu ô nhập tên.
        if ((adultGap > 0 && cleanAdultNames.size() < adultGap)
                || (childGap > 0 && cleanChildNames.size() < childGap)) {
            return new PassengerNameGap(Math.max(adultGap, 0), Math.max(childGap, 0), List.of());
        }

        TourDeparture departure = detail.getDeparture();
        int delta = (adults + children) - detail.getTotalGuests();

        if (delta > 0) {
            if (!departure.hasEnoughSeats(delta)) {
                throw new BusinessRuleException("error.checkout.notEnoughSeats",
                        detail.getTourNameSnapshot(), departure.getAvailableSeats());
            }
            departure.holdSeats(delta);
        } else if (delta < 0) {
            departure.releaseSeats(-delta);
        }

        detail.setNumAdults(adults);
        detail.setNumChildren(children);
        detail.recalculateSubtotal();
        // Tiền hàng đổi thì số tiền giảm của mã (nếu có) cũng phải tính lại theo
        // đúng luật của Promotion - xem Javadoc PromotionService.recalculateDiscount.
        // Phải chạy TRƯỚC recalculateTotal() để tổng đơn dùng đúng số giảm mới.
        promotionService.recalculateDiscount(booking);
        booking.recalculateTotal();
        // Bổ sung B: khoản thu PENDING phải đi theo tổng đơn mới, không thì màn
        // chi tiết hiện hai con số vênh nhau (xem PaymentService.syncPendingAmount).
        paymentService.syncPendingAmount(booking);

        // Giữ bất biến (mục 12.7): đồng bộ danh sách hành khách theo đúng
        // chênh lệch vừa tính - PHẢI chạy sau khi đã chắc chắn đủ chỗ/đủ tên,
        // không thì một lần gọi lỗi giữa chừng có thể để lại hành khách "mồ côi".
        // Nhẹ - 7: gom lại tên những người bị bỏ (nếu giảm số khách) để controller
        // nêu đích danh trong thông báo thành công, không để quản trị viên tự hỏi
        // "vừa xoá mất ai".
        List<String> dropped = new ArrayList<>();
        dropped.addAll(syncPassengers(detail, PassengerType.ADULT, adultGap, cleanAdultNames));
        dropped.addAll(syncPassengers(detail, PassengerType.CHILD, childGap, cleanChildNames));

        log.info("Đơn {}: sửa dòng {} thành {} người lớn, {} trẻ em (chênh lệch {} chỗ, hành khách {}A/{}C, bỏ {})",
                code, detailId, adults, children, delta, adultGap, childGap, dropped);
        return new PassengerNameGap(0, 0, dropped);
    }

    /**
     * Lọc bỏ tên rỗng và kiểm lại độ dài (Nhẹ - 4, đã tái hiện): gửi thẳng một
     * tên dài quá cột {@code booking_passengers.full_name NVARCHAR(100)} trước
     * đây rơi thẳng xuống Hibernate/JDBC, SQL Server trả lỗi cắt chuỗi và lộ
     * nguyên văn tên bảng/cột trong trang 500 - chặn sớm bằng
     * {@link BusinessRuleException} có message key để người dùng thấy một câu
     * tiếng Việt tử tế thay vì chi tiết CSDL.
     */
    private List<String> cleanNames(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> cleaned = raw.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList();
        for (String name : cleaned) {
            if (name.length() > MAX_PASSENGER_NAME_LENGTH) {
                throw new BusinessRuleException("error.checkout.passengerNameTooLong", MAX_PASSENGER_NAME_LENGTH);
            }
        }
        return cleaned;
    }

    /**
     * Đồng bộ hành khách một loại trong dòng chi tiết theo mức chênh lệch
     * {@code gap} (dương = cần thêm bấy nhiêu người tên trong {@code namesForAdd},
     * âm = cần bớt bấy nhiêu người - luôn chọn id LỚN NHẤT trước).
     */
    private List<String> syncPassengers(BookingDetail detail, PassengerType type, int gap, List<String> namesForAdd) {
        List<String> removedNames = new ArrayList<>();
        if (gap > 0) {
            for (int i = 0; i < gap; i++) {
                BookingPassenger p = new BookingPassenger(namesForAdd.get(i), type);
                p.setDetail(detail);
                detail.getPassengers().add(p);
            }
        } else if (gap < 0) {
            List<BookingPassenger> ofType = new ArrayList<>(detail.getPassengers().stream()
                    .filter(p -> p.getPassengerType() == type)
                    .toList());
            ofType.sort(Comparator.comparing(BookingPassenger::getId,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            int toRemove = -gap;
            for (int i = 0; i < toRemove && i < ofType.size(); i++) {
                BookingPassenger p = ofType.get(ofType.size() - 1 - i);
                removedNames.add(p.getFullName());
                detail.getPassengers().remove(p);
            }
        }
        return removedNames;
    }

    /**
     * Tín hiệu "vừa đặt tour xong". Chỉ mang mã đơn chứ không mang cả đối tượng
     * {@code Booking}: bên nhận chạy sau khi giao dịch đã đóng, entity lúc đó đã
     * tách khỏi ngữ cảnh lưu trữ nên đọc các quan hệ LAZY sẽ hỏng. Cầm mã đơn rồi
     * nạp lại trong một giao dịch mới là cách chắc chắn nhất.
     */
    public record BookingPlacedEvent(String bookingCode) {
    }
}
