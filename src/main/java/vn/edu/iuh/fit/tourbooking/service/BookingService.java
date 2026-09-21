package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.LocalDateTime;

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

            // isBookable() chỉ xét chính đợt khởi hành; tour "Ngừng bán" vẫn có
            // đợt active còn chỗ nên phải kiểm riêng tour.isActive() ở đây. departure
            // được nạp bằng findByIdForUpdate (không JOIN FETCH tour), nhưng đang ở
            // trong giao dịch @Transactional của phương thức này nên chạm vào quan hệ
            // LAZY departure.getTour() vẫn an toàn (khác hẳn open-in-view=false ở
            // tầng template).
            if (!departure.isBookable() || !departure.getTour().isActive()) {
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
     * Đổi trạng thái đơn, kèm trả lại hoặc giữ lại chỗ cho đúng.
     *
     * <p>Đây là phần dễ sai nhất của màn quản trị đơn hàng: huỷ đơn mà quên trả
     * chỗ thì những chỗ đó "bốc hơi" - không ai đặt được nhưng cũng không ai đi.
     * Ngược lại, khôi phục một đơn đã huỷ thì phải giữ chỗ lại, và có thể không
     * còn chỗ vì người khác đã đặt mất trong lúc đơn bị huỷ.</p>
     *
     * <p>Bổ sung A: mỗi lần đổi đều ghi thêm một dòng {@code booking_status_history}
     * - đây là chỗ duy nhất trong ứng dụng cập nhật trạng thái đơn <i>sau khi đặt</i>,
     * nên không sót lần nào.</p>
     *
     * @param changedByUserId quản trị viên đang thao tác; null nếu do hệ thống tự đổi
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

        if (newStatus == BookingStatus.CANCELLED) {
            // Huỷ đơn: trả chỗ về cho các đợt khởi hành.
            booking.getDetails().forEach(d ->
                    d.getDeparture().releaseSeats(d.getTotalGuests()));
            // Bổ sung B: đóng luôn khoản thu PENDING - không thì nút "Đã thu tiền"
            // của một đơn đã huỷ vẫn bấm được (xem PaymentService.cancelPendingForBooking).
            paymentService.cancelPendingForBooking(booking);

        } else if (oldStatus == BookingStatus.CANCELLED) {
            // Khôi phục đơn đã huỷ: phải giữ chỗ lại, và phải kiểm tra còn chỗ không.
            for (BookingDetail d : booking.getDetails()) {
                TourDeparture departure = d.getDeparture();
                if (!departure.hasEnoughSeats(d.getTotalGuests())) {
                    throw new BusinessRuleException("error.booking.restoreNoSeats",
                            d.getTourNameSnapshot(), departure.getAvailableSeats());
                }
                departure.holdSeats(d.getTotalGuests());
            }
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
