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

@Service
@RequiredArgsConstructor
@Slf4j
// Nghiệp vụ các lần thanh toán của đơn: sinh khoản PENDING, đồng bộ khi đổi số khách, đánh dấu đã thu, hoàn tiền.
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EntityManager entityManager;
    private final MessageHelper messages;

    // Suy ra enum PaymentMethod từ câu dịch hiển thị đã lưu trong booking.paymentMethod (so khớp theo ngôn ngữ hiện tại).
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

    // Sinh khoản thu PENDING ngay khi đặt tour, cùng giao dịch với đơn.
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

    // Đồng bộ số tiền của khoản PENDING theo tổng đơn mới nhất (gọi sau khi sửa số khách).
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

    // Chuyển các khoản PENDING sang FAILED khi đơn bị huỷ (PaymentStatus không có trạng thái "đã huỷ" riêng).
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

    @Transactional(readOnly = true)
    public boolean hasPaidPayment(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByIdAsc(bookingId).stream()
                .anyMatch(p -> p.getStatus() == PaymentStatus.PAID);
    }

    @Transactional(readOnly = true)
    public BigDecimal paidTotal(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByIdAsc(bookingId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Hoàn một phần trăm số tiền đã thu (nếu có), ghi thành một khoản REFUNDED riêng - không sửa khoản PAID gốc.
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

    // Đánh dấu một khoản đã thu tiền: khoá đơn (PESSIMISTIC_WRITE + refresh), kiểm đúng đơn/đúng trạng thái PENDING và mã giao dịch chưa dùng.
    @Transactional
    public void markPaid(Long paymentId, String bookingCode, String txnRef) {
        Booking booking = bookingRepository.findByCodeForUpdate(bookingCode)
                .orElseThrow(() -> ResourceNotFoundException.of("đơn đặt tour", bookingCode));
        entityManager.refresh(booking);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("error.payment.bookingCancelled", bookingCode);
        }

        Payment payment = getById(paymentId);

        if (!payment.getBooking().getCode().equals(bookingCode)) {
            throw new BusinessRuleException("error.payment.bookingMismatch", paymentId, bookingCode);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {

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
