package vn.edu.iuh.fit.tourbooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

// Danh mục tour (ví dụ "Du lịch biển"). Không map danh sách tour - lấy qua repository để phân trang.
@Entity
@Table(
        name = "tour_categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tour_categories_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_tour_categories_slug", columnNames = "slug")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TourCategory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public TourCategory(String name, String slug, String description) {
        this.name = name;
        this.slug = slug;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TourCategory other)) return false;
        return slug != null && slug.equals(other.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(slug);
    }
}
