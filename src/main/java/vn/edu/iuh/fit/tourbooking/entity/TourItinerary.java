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

// Một ngày trong lịch trình của tour (ví dụ "Ngày 2 - Vịnh Hạ Long").
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

    @Column(name = "day_no", nullable = false)
    private Integer dayNo;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "meals", length = 100)
    private String meals;

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

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
