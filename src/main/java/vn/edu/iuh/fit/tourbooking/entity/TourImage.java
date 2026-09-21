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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Một ảnh trong bộ sưu tập ảnh của tour.
 *
 * <p>{@code imagePath} lưu đường dẫn <b>tương đối</b> so với thư mục upload
 * (ví dụ {@code tours/9f3c....jpg}), không lưu đường dẫn tuyệt đối kiểu
 * {@code C:\...}: chỉ cần đổi máy hoặc đổi ổ đĩa là toàn bộ ảnh chết link.
 * URL công khai được ghép ở tầng view bằng {@code app.upload.url-prefix}.</p>
 */
@Entity
@Table(
        name = "tour_images",
        indexes = @Index(name = "idx_tour_images_tour", columnList = "tour_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TourImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tour_images_tour"))
    private Tour tour;

    @Column(name = "image_path", nullable = false, length = 255)
    private String imagePath;

    @Column(name = "caption", length = 255)
    private String caption;

    /** Thứ tự hiển thị trong thư viện ảnh; số nhỏ đứng trước. */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public TourImage(String imagePath, String caption, Integer sortOrder) {
        this.imagePath = imagePath;
        this.caption = caption;
        this.sortOrder = sortOrder;
    }

    /**
     * Ảnh không có khoá nghiệp vụ nào ngoài đường dẫn file, vốn đã là duy nhất
     * (đặt tên bằng UUID lúc lưu), nên so sánh theo {@code imagePath}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TourImage other)) return false;
        return imagePath != null && imagePath.equals(other.imagePath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(imagePath);
    }
}
