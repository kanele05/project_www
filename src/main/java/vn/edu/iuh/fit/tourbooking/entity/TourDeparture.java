package vn.edu.iuh.fit.tourbooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Một đợt khởi hành cụ thể của tour: đi ngày nào, giá bao nhiêu, còn mấy chỗ.
 *
 * <p>Đây là thứ khách hàng thực sự đặt. Việc tách khỏi {@link Tour} khiến ba
 * yêu cầu khó của đề bài trở nên có thật: chuỗi ràng buộc xoá ba tầng
 * (danh mục &rarr; tour &rarr; đợt khởi hành &rarr; chi tiết đơn), web service
 * {@code /api/tours/{id}/departures} phục vụ ô chọn ngày bằng AJAX, và nghiệp vụ
 * đặt tour có kiểm tra - trừ chỗ thật sự thay vì chỉ một câu INSERT.</p>
 */
@Entity
@Table(
        name = "tour_departures",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tour_departures_tour_date",
                columnNames = {"tour_id", "departure_date"}),
        indexes = @Index(name = "idx_tour_departures_date", columnList = "departure_date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TourDeparture extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tour_departures_tour"))
    private Tour tour;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    /** Số chỗ còn trống; giảm khi đặt thành công, tăng lại khi huỷ đơn. */
    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Column(name = "price_adult", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceAdult;

    @Column(name = "price_child", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceChild;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Khoá lạc quan chống bán quá chỗ: hai khách bấm đặt cùng lúc thì giao dịch
     * thứ hai sẽ nhận {@code OptimisticLockException} thay vì cùng ghi đè
     * {@code availableSeats} lên một giá trị sai.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public TourDeparture(Tour tour, LocalDate departureDate, LocalDate returnDate,
                         Integer totalSeats, BigDecimal priceAdult, BigDecimal priceChild) {
        this.tour = tour;
        this.departureDate = departureDate;
        this.returnDate = returnDate;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
        this.priceAdult = priceAdult;
        this.priceChild = priceChild;
    }

    // ---------------------------------------------------------------------
    // Nghiệp vụ về chỗ ngồi
    // ---------------------------------------------------------------------

    public boolean hasEnoughSeats(int seats) {
        return availableSeats != null && availableSeats >= seats;
    }

    /** Còn nhận khách: đang mở bán, chưa tới ngày đi và vẫn còn chỗ. */
    public boolean isBookable() {
        return active
                && departureDate != null
                && departureDate.isAfter(LocalDate.now())
                && availableSeats != null
                && availableSeats > 0;
    }

    /**
     * Giữ chỗ khi đặt tour. Chỉ được gọi bên trong giao dịch của
     * {@code BookingService}; việc báo lỗi thân thiện cho người dùng do tầng
     * service đảm nhiệm, ở đây chỉ chặn để dữ liệu không bao giờ âm.
     */
    public void holdSeats(int seats) {
        if (!hasEnoughSeats(seats)) {
            throw new IllegalStateException(
                    "Không đủ chỗ cho đợt khởi hành id=" + id
                            + " (còn " + availableSeats + ", cần " + seats + ")");
        }
        availableSeats -= seats;
    }

    /** Trả chỗ lại khi huỷ đơn hoặc khi giảm số khách. */
    public void releaseSeats(int seats) {
        availableSeats = Math.min(totalSeats, availableSeats + seats);
    }

    public int getBookedSeats() {
        return totalSeats - availableSeats;
    }

    /**
     * Khoá nghiệp vụ là cặp (tour, ngày khởi hành) - đúng bằng ràng buộc duy nhất
     * đã khai báo ở {@code @Table}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TourDeparture other)) return false;
        return tour != null && departureDate != null
                && tour.equals(other.tour) && departureDate.equals(other.departureDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tour == null ? null : tour.getCode(), departureDate);
    }
}
