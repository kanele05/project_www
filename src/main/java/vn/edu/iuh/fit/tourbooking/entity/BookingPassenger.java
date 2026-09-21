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

/**
 * Một hành khách cụ thể trong một dòng của đơn đặt tour.
 *
 * <p>Đây là chỗ trống lớn nhất của mô hình cũ: đơn hàng biết "2 người lớn, 1 trẻ
 * em" nhưng không biết <b>ai</b> đi. Không có bảng này thì không xuất được danh
 * sách đoàn cho hướng dẫn viên, không mua được bảo hiểm, không đặt được vé máy bay
 * hay phòng khách sạn theo tên.</p>
 *
 * <p>Quan hệ gắn vào {@link BookingDetail} chứ không gắn thẳng vào
 * {@link Booking}: một đơn có thể đặt hai tour khác nhau, và mỗi tour có danh
 * sách khách riêng.</p>
 *
 * <p>Quy tắc "số hành khách phải khớp {@code numAdults} / {@code numChildren} của
 * dòng" được kiểm ở tầng service - CSDL của đề tài không được dùng CHECK hay
 * Trigger.</p>
 */
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

    /**
     * Không dùng {@code @Enumerated}; việc quy đổi do {@code PassengerTypeConverter}
     * đảm nhiệm để Hibernate không sinh ràng buộc CHECK cho cột enum.
     */
    @Column(name = "passenger_type", nullable = false, length = 10)
    private PassengerType passengerType = PassengerType.ADULT;

    @Column(name = "gender", length = 10)
    private Gender gender;

    /** Dùng để đối chiếu giá trẻ em và mua bảo hiểm theo độ tuổi. */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** Số CCCD với tour trong nước, số hộ chiếu với tour nước ngoài. */
    @Column(name = "id_number", length = 30)
    private String idNumber;

    @Column(name = "phone", length = 20)
    private String phone;

    /** Khách đi một mình muốn ở phòng riêng - kéo theo phụ thu phòng đơn. */
    @Column(name = "single_room", nullable = false)
    private boolean singleRoom = false;

    /** Yêu cầu riêng: ăn chay, dị ứng hải sản, cần xe lăn... */
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
