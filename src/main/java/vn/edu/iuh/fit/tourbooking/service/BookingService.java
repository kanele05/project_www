package vn.edu.iuh.fit.tourbooking.service;

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
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.BookingDetail;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatusHistory;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.ResourceNotFoundException;
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
    private final ApplicationEventPublisher eventPublisher;
    private final AppProperties appProperties;
    private final MessageHelper messages;

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
            booking.addDetail(new BookingDetail(departure, item.getNumAdults(), item.getNumChildren()));
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
        Booking booking = bookingRepository.findDetailByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("đơn đặt tour", code));

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
        Booking booking = bookingRepository.findDetailByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("đơn đặt tour", code));

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
        Booking booking = bookingRepository.findDetailById(bookingId).orElse(null);
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
     * Sửa số khách của một dòng trong đơn.
     *
     * <p>Số chỗ được điều chỉnh theo <b>mức chênh lệch</b>, và tổng tiền của dòng
     * lẫn của cả đơn đều được tính lại - ba con số đó phải luôn khớp nhau, nếu
     * không thì báo cáo doanh thu và số chỗ còn trống đều sai.</p>
     */
    @Transactional
    public Booking updateDetailQuantity(String code, Long detailId, int adults, int children) {
        Booking booking = bookingRepository.findDetailByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("đơn đặt tour", code));

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

        log.info("Đơn {}: sửa dòng {} thành {} người lớn, {} trẻ em (chênh lệch {} chỗ)",
                code, detailId, adults, children, delta);
        return booking;
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
