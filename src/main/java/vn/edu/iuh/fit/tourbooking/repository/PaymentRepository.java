package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.Payment;
import vn.edu.iuh.fit.tourbooking.entity.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
// Truy vấn các lần thanh toán của đơn hàng.
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingIdOrderByIdAsc(Long bookingId);

    Optional<Payment> findByTxnRef(String txnRef);

    long countByBookingId(Long bookingId);

    long countByStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p "
            + "WHERE p.booking.id = :bookingId AND p.status = :status")
    BigDecimal sumAmountByBookingAndStatus(@Param("bookingId") Long bookingId,
                                           @Param("status") PaymentStatus status);
}
