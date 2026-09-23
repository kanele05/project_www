package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.Payment;
import vn.edu.iuh.fit.tourbooking.entity.PaymentMethod;
import vn.edu.iuh.fit.tourbooking.entity.PaymentStatus;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.tourbooking.repository.PaymentRepository;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

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

    /**
     * Sinh lại một khoản thu {@code PENDING} khi một đơn đã huỷ được khôi phục.
     *
     * <p>Trước bản vá này, {@code BookingService.updateStatus} chỉ giữ lại chỗ
     * lúc khôi phục mà không đụng gì tới {@link Payment}: khoản {@code PENDING}
     * gốc đã bị {@link #cancelPendingForBooking} chuyển sang {@code FAILED} lúc
     * huỷ, nên đơn khôi phục xong <b>có hiệu lực nhưng không còn khoản phải thu
     * nào</b> - quản trị viên không có nút "Đã thu tiền" nào để bấm nữa, khoản
     * tiền cứ thế biến mất khỏi mọi báo cáo. Sinh một dòng {@code PENDING} mới
     * theo đúng tổng tiền <b>hiện tại</b> của đơn (không phải tổng lúc đặt lần
     * đầu - đơn có thể đã bị sửa số khách trước khi huỷ).</p>
     */
    @Transactional
    public Payment restorePendingForBooking(Booking booking) {
        Payment payment = createPendingForBooking(booking);
        log.info("Đơn {}: khôi phục lại khoản thu PENDING {} đ do đơn được mở lại",
                booking.getCode(), payment.getAmount());
        return payment;
    }

    /** Đơn đã có ít nhất một khoản thu PAID - dùng để chặn sửa số khách (1.6). */
    @Transactional(readOnly = true)
    public boolean hasPaidPayment(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByIdAsc(bookingId).stream()
                .anyMatch(p -> p.getStatus() == PaymentStatus.PAID);
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
     * <p><b>Hai phép kiểm bổ sung, cả hai đều từng thiếu:</b> (1) {@code paymentId}
     * phải thuộc đúng {@code bookingCode} trên URL - không thì
     * {@code POST /admin/bookings/TB-A/payments/7/mark-paid} với khoản 7 thuộc
     * TB-B sẽ đánh dấu nhầm khoản của TB-B; (2) khoản phải đang {@code PENDING} -
     * không thì gọi lại trên khoản đã {@code PAID} sẽ ghi đè {@code paid_at}/
     * {@code txn_ref} đã đối soát.</p>
     *
     * @param bookingCode mã đơn lấy từ URL, dùng để đối chiếu quyền sở hữu khoản thu
     */
    @Transactional
    public void markPaid(Long paymentId, String bookingCode, String txnRef) {
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
