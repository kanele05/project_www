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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.iuh.fit.tourbooking.util.SlugUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Một chương trình tour được rao bán, ví dụ "Đà Nẵng - Hội An 4N3Đ".
 *
 * <p>Tour chỉ mô tả <i>chương trình</i>; ngày đi cụ thể, giá và số chỗ còn lại
 * nằm ở {@link TourDeparture}. Tách như vậy vì cùng một chương trình được bán
 * lặp lại nhiều đợt với giá khác nhau theo mùa.</p>
 *
 * <p>{@code basePrice} là giá "từ ..." dùng để hiển thị và để lọc theo khoảng giá
 * ở trang danh sách - lấy giá thật của từng đợt sẽ phải JOIN và làm hỏng phân trang.</p>
 */
@Entity
@Table(
        name = "tours",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tours_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_tours_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_tours_category", columnList = "category_id"),
                @Index(name = "idx_tours_featured", columnList = "featured"),
                @Index(name = "idx_tours_destination", columnList = "destination")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tour extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã tour do người quản trị đặt, ví dụ {@code DN-HA-4N3D}. */
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", nullable = false, length = 250)
    private String slug;

    /** Đoạn tóm tắt hiện trên thẻ tour ở trang danh sách. */
    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    /** Lịch trình từng ngày, lưu dạng văn bản nhiều dòng. */
    @Column(name = "itinerary", columnDefinition = "NVARCHAR(MAX)")
    private String itinerary;

    @Column(name = "departure_location", nullable = false, length = 100)
    private String departureLocation;

    @Column(name = "destination", nullable = false, length = 100)
    private String destination;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "duration_nights", nullable = false)
    private Integer durationNights;

    /**
     * Giá tham khảo cho một người lớn. Dùng {@code BigDecimal} chứ không dùng
     * {@code double}: số thực nhị phân không biểu diễn chính xác được tiền tệ,
     * cộng dồn nhiều dòng sẽ lệch vài đồng và không giải thích được với khách.
     */
    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;

    /**
     * Đường dẫn <b>tương đối</b> tới ảnh đại diện, ví dụ {@code tours/abc.jpg}.
     * Đây là dữ liệu lặp lại có chủ đích (đã có trong {@code tour_images}) nhằm
     * tránh bẫy N+1: trang danh sách 12 tour sẽ không phải nạp collection ảnh.
     */
    @Column(name = "thumbnail", length = 255)
    private String thumbnail;

    @Column(name = "transportation", length = 100)
    private String transportation;

    /** Tour nổi bật được đưa lên trang chủ. */
    @Column(name = "featured", nullable = false)
    private boolean featured = false;

    /** Ngừng bán nhưng vẫn giữ dữ liệu - phương án thay thế khi không xoá được. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    /**
     * Bản sao đã bỏ dấu và hạ chữ thường của tên tour, điểm đến, nơi khởi hành
     * và mã tour - phục vụ tìm kiếm không dấu.
     *
     * <p><b>Vì sao cần cột này dù CSDL đã dùng collation {@code Vietnamese_CI_AI}:</b>
     * đã kiểm chứng trực tiếp trên SQL Server, collation đó bỏ được dấu thanh và
     * dấu mũ ({@code N'à' = N'a'} trả về đúng) nhưng <b>không</b> coi {@code Đ}
     * là {@code D} ({@code N'Đ' = N'D'} trả về sai), bởi trong bảng chữ cái tiếng
     * Việt {@code Đ} là một chữ cái riêng chứ không phải {@code D} có dấu. Hệ quả
     * là gõ "da lat" sẽ không tìm ra "Đà Lạt" - đúng ngay tình huống người dùng
     * hay gặp nhất.</p>
     *
     * <p>Giá trị được dựng bằng Java ở {@link #buildSearchText()} chứ không dùng
     * hàm hay trigger của hệ quản trị CSDL, vì đề bài cấm dùng Function /
     * Stored Procedure / Trigger.</p>
     */
    @Column(name = "search_text", length = 500)
    private String searchText;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tours_category"))
    private TourCategory category;

    /** Ảnh thuộc sở hữu của tour: xoá tour thì xoá luôn ảnh. */
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<TourImage> images = new ArrayList<>();

    /**
     * Lịch trình từng ngày - cũng thuộc sở hữu của tour như ảnh.
     *
     * <p>Cột văn bản {@link #itinerary} vẫn giữ nguyên cho dữ liệu cũ; danh sách
     * này là cách biểu diễn có cấu trúc, dùng để hiện lịch trình dạng gấp mở.</p>
     */
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNo ASC, id ASC")
    private List<TourItinerary> itineraries = new ArrayList<>();

    /**
     * <b>Cố ý không có</b> {@code CascadeType.REMOVE}: nếu lỡ tay xoá một tour đã
     * có người đặt, ta muốn câu lệnh thất bại ở ràng buộc khoá ngoại chứ không
     * muốn nó âm thầm xoá luôn các đợt khởi hành đang có khách.
     */
    @OneToMany(mappedBy = "tour", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("departureDate ASC")
    private List<TourDeparture> departures = new ArrayList<>();

    /**
     * Dựng lại {@link #searchText} ngay trước mỗi lần ghi xuống CSDL.
     *
     * <p>Đặt ở vòng đời entity thay vì ở service để không bao giờ quên: dù tour
     * được sửa từ màn quản trị, từ dữ liệu mẫu hay từ bài kiểm thử, cột tìm kiếm
     * luôn khớp với dữ liệu hiện tại.</p>
     */
    @PrePersist
    @PreUpdate
    private void buildSearchText() {
        String raw = String.join(" ",
                name == null ? "" : name,
                destination == null ? "" : destination,
                departureLocation == null ? "" : departureLocation,
                code == null ? "" : code);
        this.searchText = SlugUtil.removeDiacritics(raw).toLowerCase(Locale.ROOT).trim();
    }

    // ---------------------------------------------------------------------
    // Phương thức tiện ích giữ cho hai đầu quan hệ luôn nhất quán
    // ---------------------------------------------------------------------

    public void addImage(TourImage image) {
        images.add(image);
        image.setTour(this);
    }

    public void removeImage(TourImage image) {
        images.remove(image);
        image.setTour(null);
    }

    public void addDeparture(TourDeparture departure) {
        departures.add(departure);
        departure.setTour(this);
    }

    /** Chuỗi "4 ngày 3 đêm" dùng lại ở nhiều template. */
    public String getDurationText() {
        return durationDays + " ngày " + durationNights + " đêm";
    }

    /** Số khung cảnh vẽ sẵn dùng cho tour chưa có ảnh (xem .tour-art trong app.css). */
    private static final int ART_VARIANTS = 6;

    /**
     * Chọn khung cảnh minh hoạ cho tour chưa có ảnh đại diện, trả về 0..5.
     *
     * <p><b>Băm theo điểm đến chứ không theo mã số.</b> Lúc đầu dùng
     * {@code id % 6}, nhưng các tour nổi bật lại mang mã số 1, 2, 4, 6, 8, 10 nên
     * phép chia lấy dư cho ra 1, 2, 4, 0, 2, 4 - hai cặp thẻ nằm cạnh nhau vẽ
     * trùng cảnh, nhìn thấy ngay trên trang chủ. Băm theo tên điểm đến vừa trải
     * đều hơn, vừa có ý nghĩa: mọi tour đi Đà Lạt luôn mang cùng một khung cảnh.</p>
     *
     * <p>Dùng {@code floorMod} chứ không dùng {@code %}: {@code hashCode()} trả
     * về số âm rất bình thường, mà {@code -5 % 6} trong Java bằng {@code -5} chứ
     * không phải 1 - kết quả âm sẽ sinh ra tên lớp CSS không tồn tại.</p>
     */
    public int getArtVariant() {
        String key = destination == null ? name : destination;
        return key == null ? 0 : Math.floorMod(key.hashCode(), ART_VARIANTS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tour other)) return false;
        return code != null && code.equals(other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
