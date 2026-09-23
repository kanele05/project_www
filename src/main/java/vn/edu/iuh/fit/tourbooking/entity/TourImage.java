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

// Một ảnh trong bộ sưu tập ảnh của tour, đường dẫn tương đối so với thư mục upload.
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

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public TourImage(String imagePath, String caption, Integer sortOrder) {
        this.imagePath = imagePath;
        this.caption = caption;
        this.sortOrder = sortOrder;
    }

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
