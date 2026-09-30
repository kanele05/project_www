package vn.edu.iuh.fit.tourbooking.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Một dòng trong đơn: lịch khởi hành nào, mấy khách, tên tour và đơn giá được chép lại tại thời điểm đặt.
@Entity
@Table(
        name = "booking_details",
        indexes = {
                @Index(name = "idx_booking_details_booking", columnList = "booking_id"),
                @Index(name = "idx_booking_details_departure", columnList = "departure_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_booking_details_booking"))
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departure_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_booking_details_departure"))
    private TourDeparture departure;

    @Column(name = "tour_name_snapshot", nullable = false, length = 200)
    private String tourNameSnapshot;

    @Column(name = "num_adults", nullable = false)
    private Integer numAdults = 1;

    @Column(name = "num_children", nullable = false)
    private Integer numChildren = 0;

    @Column(name = "unit_price_adult", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPriceAdult;

    @Column(name = "unit_price_child", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPriceChild;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "detail", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("passengerType ASC, id ASC")
    private List<BookingPassenger> passengers = new ArrayList<>();

    public BookingDetail(TourDeparture departure, int numAdults, int numChildren) {
        this.departure = departure;
        this.tourNameSnapshot = departure.getTour().getName();
        this.numAdults = numAdults;
        this.numChildren = numChildren;
        this.unitPriceAdult = departure.getPriceAdult();
        this.unitPriceChild = departure.getPriceChild();
        recalculateSubtotal();
    }

    public int getTotalGuests() {
        return numAdults + numChildren;
    }

    // Tính lại thành tiền của dòng này theo đơn giá và số khách hiện có.
    public void recalculateSubtotal() {
        BigDecimal adults = unitPriceAdult.multiply(BigDecimal.valueOf(numAdults));
        BigDecimal children = unitPriceChild.multiply(BigDecimal.valueOf(numChildren));
        this.subtotal = adults.add(children);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookingDetail other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
