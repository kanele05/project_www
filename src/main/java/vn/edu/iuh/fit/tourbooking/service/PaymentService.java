package vn.edu.iuh.fit.tourbooking.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;
import vn.edu.iuh.fit.tourbooking.entity.Payment;
import vn.edu.iuh.fit.tourbooking.entity.PaymentMethod;
import vn.edu.iuh.fit.tourbooking.entity.PaymentStatus;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.tourbooking.repository.BookingRepository;
import vn.edu.iuh.fit.tourbooking.repository.PaymentRepository;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Nghiệp vụ thanh toán (Bổ sung B, ghi vào UC007 - không phải use case riêng).
 *
 * <p>Bảng {@code payments} tồn tại từ Phase 7 nhưng chưa từng được ghi vào cho
 * tới khi vá theo {@code docs/report/SPEC_CHUNG.md} mục 9: checkout sinh sẵn
 * một dòng {@code PENDING}, quản trị viên đánh dấu đã thu tiền thì chuyển
 * {@code PAID}.</p>
 *
 * <p><b>{@code txnRef} không có ràng buộc UNIQUE dưới CSDL</b> - SQL Server chỉ
 * chấp nhận đúng một dòng NULL cho một cột UNIQUE, mà phần lớn các dòng
 * {@code PENDING} đều chưa có mã giao dịch (xem gotcha #41, và Javadoc của
 * {@link Payment#txnRef}). Tính duy nhất được kiểm ở đây, bằng Java.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EntityManager entityManager;
    private final MessageHelper messages;

    /**
     * Nhận diện hình thức thanh toán từ chuỗi hiển thị mà khách chọn ở trang
     * thanh toán ({@code booking.paymentMethod} lưu nguyên văn câu đã dịch, xem
     * {@code checkout.html}). So khớp với câu dịch hiện tại theo đúng ngôn ngữ
     * của request - không đổi cấu trúc {@code CheckoutForm}/{@code checkout.html}
     * chỉ để có một mã hình thức thanh toán "sạch".
     */
    private PaymentMethod resolveMethod(String displayText) {
        if (displayText == null) {
            return PaymentMethod.BANK_TRANSFER;
        }
        String trimmed = displayText.trim();
        if (trimmed.equals(messages.get("checkout.payment.cash"))) {
            return PaymentMethod.CASH;
        }
        if (trimmed.equals(messages.get("checkout.payment.momo"))) {
            return PaymentMethod.MOMO;
        }
        return PaymentMethod.BANK_TRANSFER;
    }

    /**
     * Sinh dòng thanh toán {@code PENDING} ngay khi đặt tour - gọi trong cùng
     * giao dịch với {@code BookingService.placeOrder}: đơn hỏng thì dòng thanh
     * toán cũng biến mất theo, không có chuyện tồn tại một khoản phải thu cho
     * một đơn không có thật.
     */
    @Transactional
    public Payment createPendingForBooking(Booking booking) {
        Payment payment = new Payment(booking, booking.getTotalAmount(), resolveMethod(booking.getPaymentMethod()));
        Payment saved = paymentRepository.save(payment);
        log.info("Đơn {} sinh khoản thu {} đ ({})",
                booking.getCode(), saved.getAmount(), saved.getMethod());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Payment> findByBookingId(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByIdAsc(bookingId);
    }

    /**
     * Đồng bộ lại khoản thu {@code PENDING} của một đơn cho khớp tổng tiền mới.
     *
     * <p>Gọi ngay sau {@code Booking.recalculateTotal()} trong
     * {@code BookingService.updateDetailQuantity}: trước bản vá này, sửa số khách
     * đổi tổng đơn nhưng khoản thu (sinh một lần duy nhất lúc đặt) đứng yên, màn
     * chi tiết hiện hai con số vênh nhau và "Đã thu tiền" đóng khoản thiếu/thừa.
     * Chỉ đụng khoản còn {@code PENDING} - khoản đã {@code PAID} là tiền đã thu
     * thật, không được âm thầm sửa lại.</p>
     */
    @Transactional
    public void syncPendingAmount(Booking booking) {
        paymentRepository.findByBookingIdOrderByIdAsc(booking.getId()).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .forEach(p -> {
                    p.setAmount(booking.getTotalAmount());
                    log.info("Đơn {}: đồng bộ khoản thu PENDING id={} thành {} đ",
                            booking.getCode(), p.getId(), p.getAmount());
                });
    }

    /**
     * Đóng mọi khoản thu còn {@code PENDING} của một đơn vừa bị huỷ.
     *
     * <p>Trước bản vá này, huỷ đơn không đụng tới khoản {@code PENDING} sinh ra
     * lúc đặt - nút "Đã thu tiền" của một đơn đã huỷ vẫn bấm được, ghi nhận thu
     * tiền cho một đơn không còn hiệu lực. Không có trạng thái "đã huỷ" riêng cho
     * {@link Payment}; dùng lại {@link PaymentStatus#FAILED} (khoản không còn
     * được thu) thay vì thêm một hằng số enum mới.</p>
     */
    @Transactional
    public void cancelPendingForBooking(Booking booking) {
        paymentRepository.findByBookingIdOrderByIdAsc(booking.getId()).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .forEach(p -> {
                    p.setStatus(PaymentStatus.FAILED);
                    p.setNote(messages.get("payment.note.cancelledByBookingCancel"));
                    log.info("Đơn {}: đóng khoản thu PENDING id={} do đơn bị huỷ",
                            booking.getCode(), p.getId());
                });
    }

    /** Đơn đã có ít nhất một khoản thu PAID - dùng để chặn sửa số khách (1.6). */
    @Transactional(readOnly = true)
    public boolean hasPaidPayment(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByIdAsc(bookingId).stream()
                .anyMatch(p -> p.getStatus() == PaymentStatus.PAID);
    }

    /** Tổng các khoản ĐÃ THANH TOÁN của một đơn - dùng để tính số tiền hoàn (mục 12.2/12.3). */
    @Transactional(readOnly = true)
    public BigDecimal paidTotal(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByIdAsc(bookingId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Hoàn tiền khi huỷ đơn đã có khoản ĐÃ THANH TOÁN (mục 12.2) - <b>không đổi
     * schema</b>: dòng PAID gốc được giữ nguyên làm lịch sử tiền vào, phương thức
     * này chỉ THÊM một dòng {@code REFUNDED} mới với số tiền hoàn (luôn dương).
     *
     * <p>Không làm gì nếu đơn chưa từng có khoản PAID nào, hoặc {@code refundPercent}
     * bằng 0 (ví dụ khách tự huỷ nhưng chưa hề thanh toán đồng nào thì không có gì
     * để hoàn) - tránh sinh ra một dòng {@code REFUNDED} với số tiền 0 vô nghĩa.</p>
     *
     * @param refundPercent tỉ lệ hoàn, 0-100. Quản trị viên huỷ luôn truyền 100
     *                       (mục 12.2); khách tự huỷ truyền theo chính sách 12.3.
     * @param note           lý do + tỉ lệ, đã dịch sẵn theo ngôn ngữ hiện tại
     */
    @Transactional
    public void refundIfPaid(Booking booking, int refundPercent, String note) {
        if (refundPercent <= 0) {
            return;
        }
        BigDecimal paidTotal = paidTotal(booking.getId());
        if (paidTotal.signum() <= 0) {
            return;
        }
        BigDecimal refundAmount = paidTotal
                .multiply(BigDecimal.valueOf(refundPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        if (refundAmount.signum() <= 0) {
            return;
        }

        Payment refund = new Payment(booking, refundAmount, resolveMethod(booking.getPaymentMethod()));
        refund.setStatus(PaymentStatus.REFUNDED);
        refund.setPaidAt(LocalDateTime.now());
        refund.setNote(note);
        paymentRepository.save(refund);

        log.info("Đơn {}: hoàn {} đ ({}% của {} đ đã thu) - {}",
                booking.getCode(), refundAmount, refundPercent, paidTotal, note);
    }

    @Transactional(readOnly = true)
    public Payment getById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("khoản thanh toán", id));
    }

    /**
     * Quản trị viên đánh dấu đã thu tiền.
     *
     * <p>Kiểm trùng {@code txnRef} bằng Java - đúng lý do đã giải thích ở
     * Javadoc lớp này. Mã trống thì bỏ qua phép kiểm: nhiều lần thanh toán tiền
     * mặt hoàn toàn có thể không có mã giao dịch nào cả.</p>
     *
     * <p><b>Ba phép kiểm bổ sung:</b> (1) {@code paymentId} phải thuộc đúng
     * {@code bookingCode} trên URL - không thì
     * {@code POST /admin/bookings/TB-A/payments/7/mark-paid} với khoản 7 thuộc
     * TB-B sẽ đánh dấu nhầm khoản của TB-B; (2) khoản phải đang {@code PENDING} -
     * không thì gọi lại trên khoản đã {@code PAID} sẽ ghi đè {@code paid_at}/
     * {@code txn_ref} đã đối soát; (3) <b>đơn cha không được đang/vừa
     * {@code CANCELLED}</b> (mục "NGHIÊM TRỌNG - 1", kịch bản A đã tái hiện bằng
     * thao tác thật: khách tự huỷ trùng lúc admin bấm "Đã thu tiền" từng khiến
     * khoản thu bị ghi đè PAID rồi FAILED, mất dấu tiền đã thu).</p>
     *
     * <p><b>Khoá đơn cha TRƯỚC khi đụng khoản thu</b> - đúng thứ tự khoá "đơn
     * trước" đã thống nhất với {@code BookingService} (xem
     * {@code BookingService.lockBookingByCode}): mọi phương thức có thể huỷ đơn
     * này (giải phóng chỗ, đóng khoản PENDING, hoàn tiền) đều khoá đúng dòng
     * {@code bookings} này trước tiên, nên bên thua ở đây cũng phải đợi rồi đọc
     * lại đúng trạng thái mới nhất. {@code Payment} không có {@code @Version}
     * riêng nên toàn bộ tính đúng đắn dựa hẳn vào khoá của đơn cha - tải
     * {@link Payment} SAU khi đã giữ được khoá (không tải trước) để tránh đọc
     * nhầm bản cũ trong cache cấp một (gotcha #60).</p>
     *
     * @param bookingCode mã đơn lấy từ URL, dùng để khoá đơn cha và đối chiếu quyền sở hữu khoản thu
     */
    @Transactional
    public void markPaid(Long paymentId, String bookingCode, String txnRef) {
        Booking booking = bookingRepository.findByCodeForUpdate(bookingCode)
                .orElseThrow(() -> ResourceNotFoundException.of("đơn đặt tour", bookingCode));
        entityManager.refresh(booking);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("error.payment.bookingCancelled", bookingCode);
        }

        // Tải Payment SAU khi đã khoá đơn cha - xem Javadoc phía trên.
        Payment payment = getById(paymentId);

        if (!payment.getBooking().getCode().equals(bookingCode)) {
            throw new BusinessRuleException("error.payment.bookingMismatch", paymentId, bookingCode);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            // messages.get(status.getMessageKey()) chứ không phải getDisplayName():
            // getDisplayName() trả cứng câu tiếng Việt viết sẵn trong enum, nên một
            // quản trị viên đang xem giao diện tiếng Anh vẫn nhận được thông báo lỗi
            // tiếng Việt giữa các câu chữ còn lại đã dịch (gotcha kiểu #9 cũ).
            throw new BusinessRuleException("error.payment.notPending",
                    messages.get(payment.getStatus().getMessageKey()));
        }

        String normalized = (txnRef == null || txnRef.isBlank()) ? null : txnRef.trim();

        if (normalized != null) {
            paymentRepository.findByTxnRef(normalized)
                    .filter(existing -> !existing.getId().equals(paymentId))
                    .ifPresent(existing -> {
                        throw new BusinessRuleException("error.payment.txnRefExists", normalized);
                    });
        }

        payment.markPaid(normalized);
        log.info("Khoản thanh toán id={} (đơn {}) đã được đánh dấu thu tiền",
                paymentId, payment.getBooking().getCode());
    }
}
