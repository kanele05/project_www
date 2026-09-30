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

@Service
@RequiredArgsConstructor
@Slf4j
// Nghiệp vụ trung tâm của đơn hàng: đặt tour, máy trạng thái, tự huỷ/huỷ bởi khách, hoàn tiền, sửa số khách.
public class BookingService {

    public static final int PAGE_SIZE = 10;

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

    private static final int MAX_PASSENGER_NAME_LENGTH = 100;

    // Khoá bi quan một đơn theo mã rồi refresh - bắt buộc để không đọc nhầm state cũ trong session (gotcha #60).
    private Booking lockBookingByCode(String code) {
        Booking locked = bookingRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> ResourceNotFoundException.of("đơn đặt tour", code));
        entityManager.refresh(locked);
        return locked;
    }

    // Khoá bi quan một đơn theo id rồi refresh; trả về null nếu không còn tồn tại.
    private Booking lockBookingById(Long id) {
        Booking locked = bookingRepository.findByIdForUpdate(id).orElse(null);
        if (locked == null) {
            return null;
        }
        entityManager.refresh(locked);
        return locked;
    }

    // Đặt tour từ giỏ hàng: khoá và trừ chỗ từng lịch khởi hành, ghi hành khách, áp mã giảm giá, sinh khoản thu PENDING và dòng lịch sử đầu tiên, phát sự kiện gửi thư.
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

        booking.setCustomerName(form.getCustomerName().trim());
        booking.setCustomerEmail(form.getCustomerEmail().trim());
        booking.setCustomerPhone(form.getCustomerPhone().trim());
        booking.setCustomerAddress(form.getCustomerAddress());
        booking.setPaymentMethod(form.getPaymentMethod());
        booking.setNote(form.getNote());

        for (CartItem item : cart.getItems()) {

            TourDeparture departure = departureRepository.findByIdForUpdate(item.getDepartureId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "error.checkout.departureGone", item.getTourName()));

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

            attachPassengers(detail, findGroup(form, item.getDepartureId()), item);

            booking.addDetail(detail);
        }

        PromotionService.CouponCheck coupon = null;
        if (form.getCouponCode() != null && !form.getCouponCode().isBlank()) {
            coupon = promotionService.check(form.getCouponCode(), userId, booking.getSubtotalAmount());
            booking.setPromotion(coupon.promotion());
            booking.setDiscountAmount(coupon.discount());
        }

        booking.recalculateTotal();
        Booking saved = bookingRepository.save(booking);

        if (coupon != null) {

            promotionService.recordUsage(coupon.promotion(), user, saved, coupon.discount());
        }

        bookingStatusHistoryRepository.save(
                new BookingStatusHistory(saved, null, BookingStatus.PENDING, user, null));

        paymentService.createPendingForBooking(saved);

        eventPublisher.publishEvent(new BookingPlacedEvent(saved.getCode()));

        log.info("Đã tạo đơn {} cho {} - {} dòng, tổng {} đ",
                saved.getCode(), user.getEmail(), saved.getDetails().size(), saved.getTotalAmount());
        return saved;
    }

    // Tìm nhóm hành khách đã nhập trên form ứng với một lịch khởi hành trong giỏ.
    private PassengerGroupForm findGroup(CheckoutForm form, Long departureId) {
        if (form.getPassengerGroups() == null) {
            return null;
        }
        return form.getPassengerGroups().stream()
                .filter(g -> departureId.equals(g.getDepartureId()))
                .findFirst()
                .orElse(null);
    }

    // Gắn danh sách hành khách vào dòng chi tiết; số lượng nhập phải khớp đúng số người lớn/trẻ em đã đặt.
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

    // Dựng một BookingPassenger từ dữ liệu form, kiểm họ tên bắt buộc và ngày sinh không ở tương lai.
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

    @Transactional(readOnly = true)
    public Page<Booking> findByUser(Long userId, int page) {
        return bookingRepository.findByUserIdOrderByBookingDateDesc(
                userId, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    // Lấy đơn theo mã kèm kiểm quyền sở hữu: không phải admin và không phải chủ đơn thì bị chặn.
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

    @Transactional(readOnly = true)
    public Booking getDetailByCode(String code) {
        return bookingRepository.findDetailByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("đơn đặt tour", code));
    }

    @Transactional(readOnly = true)
    public Page<Booking> adminSearch(String keyword, BookingStatus status, int page) {
        return bookingRepository.adminSearch(keyword, status,

                PageRequest.of(Math.max(page, 0), ADMIN_PAGE_SIZE,
                        Sort.by(Sort.Direction.DESC, "bookingDate")));
    }

    // Đổi trạng thái đơn theo máy trạng thái; CANCELLED thì trả chỗ + huỷ khoản thu + hoàn 100%, COMPLETED phải qua điều kiện riêng. Luôn ghi một dòng lịch sử.
    @Transactional
    public Booking updateStatus(String code, BookingStatus newStatus, Long changedByUserId, String reason) {

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

            releaseHold(booking);

            paymentService.cancelPendingForBooking(booking);

            paymentService.refundIfPaid(booking, 100, messages.get("payment.note.refund.adminCancel"));
        }

        booking.setStatus(newStatus);

        User changedBy = changedByUserId == null ? null : userRepository.getReferenceById(changedByUserId);
        bookingStatusHistoryRepository.save(
                new BookingStatusHistory(booking, oldStatus, newStatus, changedBy,
                        (reason == null || reason.isBlank()) ? null : reason.trim()));

        log.info("Đơn {}: {} -> {}", code, oldStatus, newStatus);
        return booking;
    }

    @Transactional(readOnly = true)
    // Danh sách trạng thái hợp lệ có thể chuyển tới từ trạng thái hiện tại, để vẽ nút trên giao diện.
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

    // Đủ điều kiện chuyển sang COMPLETED không: đã tới ngày khởi hành và đã có khoản PAID.
    private boolean isCompletable(Booking booking) {
        return !departureNotYetReached(booking) && paymentService.hasPaidPayment(booking.getId());
    }

    // Như isCompletable nhưng ném lỗi kèm lý do cụ thể nếu chưa đủ điều kiện.
    private void requireCompletable(Booking booking) {
        if (departureNotYetReached(booking)) {
            throw new BusinessRuleException("error.booking.complete.notYetDeparted");
        }
        if (!paymentService.hasPaidPayment(booking.getId())) {
            throw new BusinessRuleException("error.booking.complete.notPaid");
        }
    }

    private boolean departureNotYetReached(Booking booking) {
        return booking.getDetails().stream()
                .map(d -> d.getDeparture().getDepartureDate())
                .anyMatch(date -> date.isAfter(LocalDate.now()));
    }

    // Trả lại chỗ đã giữ ở mọi lịch khởi hành của đơn và trả lượt dùng mã giảm giá (nếu có).
    private void releaseHold(Booking booking) {
        booking.getDetails().forEach(d -> d.getDeparture().releaseSeats(d.getTotalGuests()));
        promotionService.releaseUsage(booking);
    }

    public record SelfCancelPolicy(boolean timingOk, int refundPercent,
                                   BigDecimal refundAmount, long daysUntilDeparture) {
    }

    @Transactional(readOnly = true)
    // Tính chính sách tự huỷ theo số ngày còn lại tới ngày khởi hành sớm nhất: có kịp huỷ không, hoàn bao nhiêu %.
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

    // Khách tự huỷ đơn của chính mình: kiểm quyền sở hữu, kiểm còn huỷ được và còn kịp thời hạn, trả chỗ và hoàn tiền theo chính sách.
    @Transactional
    public Booking cancelBySelf(String code, Long userId) {

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

    // Tự huỷ một đơn PENDING quá hạn thanh toán (gọi từ BookingExpiryScheduler); bỏ qua nếu đã có khoản PAID.
    @Transactional
    public void expirePendingBooking(Long bookingId) {

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

        bookingStatusHistoryRepository.save(new BookingStatusHistory(booking, oldStatus,
                BookingStatus.CANCELLED, null, "Quá hạn thanh toán"));

        log.info("Đơn {} tự huỷ do quá hạn thanh toán (quá {} giờ)",
                booking.getCode(), appProperties.booking().pendingExpiryHours());
    }

    public record PassengerNameGap(int adultsNeeded, int childrenNeeded, List<String> droppedNames) {
        public boolean isEmpty() {
            return adultsNeeded <= 0 && childrenNeeded <= 0;
        }
    }

    // Admin sửa số khách của một dòng: chặn nếu đơn ở trạng thái cuối hoặc đã có khoản PAID; điều chỉnh chỗ, tiền, giảm giá và đồng bộ danh sách hành khách theo tên mới nếu tăng số khách.
    @Transactional
    public PassengerNameGap updateDetailQuantity(String code, Long detailId, int adults, int children,
                                                  List<String> newAdultNames, List<String> newChildNames) {

        Booking booking = lockBookingByCode(code);

        if (booking.getStatus().isFinal()) {
            throw new BusinessRuleException("error.booking.finalStatus");
        }

        if (paymentService.hasPaidPayment(booking.getId())) {
            throw new BusinessRuleException("error.booking.detailQuantity.hasPaidPayment");
        }
        if (adults < 1) {
            throw new BusinessRuleException("error.cart.needAdult");
        }

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

        promotionService.recalculateDiscount(booking);
        booking.recalculateTotal();

        paymentService.syncPendingAmount(booking);

        List<String> dropped = new ArrayList<>();
        dropped.addAll(syncPassengers(detail, PassengerType.ADULT, adultGap, cleanAdultNames));
        dropped.addAll(syncPassengers(detail, PassengerType.CHILD, childGap, cleanChildNames));

        log.info("Đơn {}: sửa dòng {} thành {} người lớn, {} trẻ em (chênh lệch {} chỗ, hành khách {}A/{}C, bỏ {})",
                code, detailId, adults, children, delta, adultGap, childGap, dropped);
        return new PassengerNameGap(0, 0, dropped);
    }

    // Lọc bỏ tên rỗng, cắt khoảng trắng thừa, kiểm độ dài tối đa.
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

    // Đồng bộ danh sách hành khách theo loại khi số khách đổi: gap dương thì thêm khách mới theo tên nhập, gap âm thì bớt khách cuối danh sách.
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

    public record BookingPlacedEvent(String bookingCode) {
    }
}
