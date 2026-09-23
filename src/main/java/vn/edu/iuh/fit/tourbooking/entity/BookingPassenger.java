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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

// Một hành khách cụ thể của một dòng chi tiết đơn (họ tên, giấy tờ, ngày sinh...).
@Entity
@Table(
        name = "booking_passengers",
        indexes = @Index(name = "idx_booking_passengers_detail", columnList = "detail_id")
)
@Getter
@Setter
@NoArgsConstructor
public class BookingPassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detail_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_booking_passengers_detail"))
    private BookingDetail detail;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "passenger_type", nullable = false, length = 10)
    private PassengerType passengerType = PassengerType.ADULT;

    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "id_number", length = 30)
    private String idNumber;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "single_room", nullable = false)
    private boolean singleRoom = false;

    @Column(name = "note", length = 255)
    private String note;

    public BookingPassenger(String fullName, PassengerType passengerType) {
        this.fullName = fullName;
        this.passengerType = passengerType;
    }

    public boolean isAdult() {
        return passengerType == PassengerType.ADULT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookingPassenger other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
