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

/**
 * Một dòng trong đơn đặt tour: khách đặt đợt khởi hành nào, mấy người lớn,
 * mấy trẻ em, thành tiền bao nhiêu.
 *
 * <p>Tên tour và đơn giá được <b>chép lại</b> tại thời điểm đặt. Nếu chỉ trỏ sang
 * {@link TourDeparture} thì khi quản trị viên chỉnh giá cho đợt sau, hoá đơn cũ
 * sẽ đổi số theo - sai về mặt kế toán và không giải thích được với khách.</p>
 *
 * <p>Đây cũng là bảng chặn xoá: còn dòng nào trỏ tới một đợt khởi hành thì không
 * được xoá đợt đó, và theo đó cũng không xoá được tour hay danh mục ở trên.</p>
 */
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

    /** Bản sao tên tour lúc đặt, để hoá đơn cũ không đổi khi tour bị đổi tên. */
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

    /**
     * Thành tiền của dòng. Lưu sẵn thay vì tính lại mỗi lần đọc để báo cáo doanh
     * thu chỉ cần {@code SUM(subtotal)}; giá trị luôn được làm mới bằng
     * {@link #recalculateSubtotal()} sau khi đổi số lượng khách.
     */
    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /**
     * Danh sách hành khách của dòng này. Thuộc sở hữu của dòng chi tiết nên
     * {@code cascade = ALL} + {@code orphanRemoval}: sửa lại số khách rồi bỏ bớt
     * người khỏi danh sách là xoá luôn bản ghi.
     */
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

    /** {@code subtotal = numAdults * giá người lớn + numChildren * giá trẻ em}. */
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

    /**
     * Hằng số thay vì {@code Objects.hash(id)}: bản ghi mới chưa có id, nếu
     * hashCode đổi sau khi lưu thì đối tượng sẽ "biến mất" khỏi HashSet.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
