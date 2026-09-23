package vn.edu.iuh.fit.tourbooking.session;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
// Một dòng trong giỏ hàng: chép lại thông tin tour/đợt khởi hành tại thời điểm thêm vào giỏ.
public class CartItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long departureId;

    private Long tourId;
    private String tourCode;
    private String tourName;
    private String tourSlug;
    private String destination;

    private String thumbnail;

    private LocalDate departureDate;
    private LocalDate returnDate;
    private Integer durationDays;
    private Integer durationNights;

    private BigDecimal priceAdult;
    private BigDecimal priceChild;

    private int numAdults;
    private int numChildren;

    public CartItem(TourDeparture departure, int numAdults, int numChildren) {
        this.departureId = departure.getId();
        this.departureDate = departure.getDepartureDate();
        this.returnDate = departure.getReturnDate();
        this.priceAdult = departure.getPriceAdult();
        this.priceChild = departure.getPriceChild();
        this.numAdults = numAdults;
        this.numChildren = numChildren;

        this.tourId = departure.getTour().getId();
        this.tourCode = departure.getTour().getCode();
        this.tourName = departure.getTour().getName();
        this.tourSlug = departure.getTour().getSlug();
        this.destination = departure.getTour().getDestination();
        this.thumbnail = departure.getTour().getThumbnail();
        this.durationDays = departure.getTour().getDurationDays();
        this.durationNights = departure.getTour().getDurationNights();
    }

    public int getQuantity() {
        return numAdults + numChildren;
    }

    public BigDecimal getSubtotal() {
        BigDecimal adults = priceAdult.multiply(BigDecimal.valueOf(numAdults));
        BigDecimal children = priceChild.multiply(BigDecimal.valueOf(numChildren));
        return adults.add(children);
    }

    public String getDurationText() {
        return durationDays + " ngày " + durationNights + " đêm";
    }

    // Cộng dồn số khách khi thêm cùng một đợt khởi hành vào giỏ lần nữa.
    public void merge(int moreAdults, int moreChildren) {
        this.numAdults += moreAdults;
        this.numChildren += moreChildren;
    }
}
