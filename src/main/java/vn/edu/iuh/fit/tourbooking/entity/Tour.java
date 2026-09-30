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

// Chương trình tour được rao bán; ngày đi, giá, số chỗ cụ thể nằm ở TourDeparture.
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

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", nullable = false, length = 250)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

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

    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "thumbnail", length = 255)
    private String thumbnail;

    @Column(name = "transportation", length = 100)
    private String transportation;

    @Column(name = "featured", nullable = false)
    private boolean featured = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "search_text", length = 500)
    private String searchText;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tours_category"))
    private TourCategory category;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<TourImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNo ASC, id ASC")
    private List<TourItinerary> itineraries = new ArrayList<>();

    @OneToMany(mappedBy = "tour", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("departureDate ASC")
    private List<TourDeparture> departures = new ArrayList<>();

    // Dựng cột tìm kiếm không dấu, chữ thường trước khi lưu (collation không coi Đ = D).
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

    // Thêm ảnh và gắn ngược lại tham chiếu tour.
    public void addImage(TourImage image) {
        images.add(image);
        image.setTour(this);
    }

    // Gỡ một ảnh khỏi tour.
    public void removeImage(TourImage image) {
        images.remove(image);
        image.setTour(null);
    }

    // Thêm một lịch khởi hành và gắn ngược lại tham chiếu tour.
    public void addDeparture(TourDeparture departure) {
        departures.add(departure);
        departure.setTour(this);
    }

    public String getDurationText() {
        return durationDays + " ngày " + durationNights + " đêm";
    }

    private static final int ART_VARIANTS = 6;

    // Chọn một trong sáu kiểu nền gradient khi tour chưa có ảnh, theo hash của điểm đến.
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
