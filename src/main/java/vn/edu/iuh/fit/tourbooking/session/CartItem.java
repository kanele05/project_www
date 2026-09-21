package vn.edu.iuh.fit.tourbooking.session;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Một dòng trong giỏ hàng: khách chọn đợt khởi hành nào, mấy người lớn, mấy trẻ em.
 *
 * <p><b>Đây là POJO thuần, không phải entity JPA.</b> Đề bài yêu cầu giỏ hàng
 * phải nằm trong Session, nên lớp này chỉ cần {@link Serializable} để servlet
 * container có thể ghi session ra đĩa khi khởi động lại. Trong CSDL <b>không</b>
 * có bảng nào tương ứng - kiểm chứng bằng
 * {@code SELECT * FROM sys.tables WHERE name LIKE '%cart%'} trả về 0 dòng.</p>
 *
 * <p>Mọi thông tin hiển thị (tên tour, ảnh, giá) đều được chép sẵn vào đây lúc
 * thêm vào giỏ. Nhờ vậy trang giỏ hàng render được mà không cần truy vấn CSDL,
 * và cũng không dính {@code LazyInitializationException} do
 * {@code open-in-view = false}. Giá thật vẫn được đọc lại từ CSDL ở bước thanh
 * toán, nên khách không thể sửa giá bằng cách can thiệp vào session.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class CartItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Khoá của dòng trong giỏ - trùng đợt khởi hành thì gộp chứ không thêm dòng mới. */
    private Long departureId;

    private Long tourId;
    private String tourCode;
    private String tourName;
    private String tourSlug;
    private String destination;

    /** Đường dẫn tương đối của ảnh đại diện, có thể null nếu tour chưa có ảnh. */
    private String thumbnail;

    private LocalDate departureDate;
    private LocalDate returnDate;
    private Integer durationDays;
    private Integer durationNights;

    private BigDecimal priceAdult;
    private BigDecimal priceChild;

    private int numAdults;
    private int numChildren;

    /**
     * Dựng một dòng giỏ hàng từ đợt khởi hành.
     *
     * <p>Nơi gọi phải nạp sẵn {@code departure.tour} (dùng
     * {@code findWithTourById}), vì mọi quan hệ {@code @ManyToOne} đều LAZY.</p>
     */
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

    /**
     * Số lượng của dòng, hiểu là <b>tổng số khách</b>.
     *
     * <p>Đề bài yêu cầu "sửa được số lượng, số lượng bằng 0 thì xoá khỏi giỏ".
     * Một chuyến đi không có ai đi thì không còn là một dòng hợp lệ, nên quy ước
     * số lượng = số người lớn + số trẻ em, và {@code CartService} xoá dòng khi
     * giá trị này về 0.</p>
     */
    public int getQuantity() {
        return numAdults + numChildren;
    }

    /** Thành tiền của dòng. */
    public BigDecimal getSubtotal() {
        BigDecimal adults = priceAdult.multiply(BigDecimal.valueOf(numAdults));
        BigDecimal children = priceChild.multiply(BigDecimal.valueOf(numChildren));
        return adults.add(children);
    }

    public String getDurationText() {
        return durationDays + " ngày " + durationNights + " đêm";
    }

    /** Cộng thêm khách khi khách đặt lại đúng đợt khởi hành đã có trong giỏ. */
    public void merge(int moreAdults, int moreChildren) {
        this.numAdults += moreAdults;
        this.numChildren += moreChildren;
    }
}
