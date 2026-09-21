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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một ngày trong lịch trình của tour: "Ngày 2 - Vịnh Hạ Long".
 *
 * <p>Trước đây toàn bộ lịch trình nằm gọn trong một cột văn bản
 * {@code tours.itinerary}. Cột đó vẫn giữ lại cho các tour cũ, nhưng dữ liệu mới
 * nên nhập vào bảng này: có tách ra thì mới hiện được lịch trình dạng gấp mở từng
 * ngày, mới lọc được "tour có ngày tự do", và mới sửa được một ngày mà không phải
 * chép tay lại cả đoạn văn bản.</p>
 *
 * <p>Ràng buộc duy nhất {@code (tour_id, day_no)}: một tour không thể có hai
 * "ngày 2".</p>
 */
@Entity
@Table(
        name = "tour_itineraries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tour_itineraries_tour_day", columnNames = {"tour_id", "day_no"}),
        indexes = @Index(name = "idx_tour_itineraries_tour", columnList = "tour_id")
)
@Getter
@Setter
@NoArgsConstructor
public class TourItinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tour_itineraries_tour"))
    private Tour tour;

    /** Ngày thứ mấy của hành trình, bắt đầu từ 1. */
    @Column(name = "day_no", nullable = false)
    private Integer dayNo;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    /** Các bữa ăn trong ngày, ví dụ "Sáng, Trưa, Tối". */
    @Column(name = "meals", length = 100)
    private String meals;

    /** Nơi nghỉ đêm, ví dụ "Khách sạn 4 sao tại Hạ Long". */
    @Column(name = "accommodation", length = 150)
    private String accommodation;

    public TourItinerary(Tour tour, Integer dayNo, String title, String description) {
        this.tour = tour;
        this.dayNo = dayNo;
        this.title = title;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TourItinerary other)) return false;
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
