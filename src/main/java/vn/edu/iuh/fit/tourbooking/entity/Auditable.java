package vn.edu.iuh.fit.tourbooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Lớp cha chứa hai mốc thời gian mà hầu như bảng nào cũng cần.
 *
 * <p>Dùng {@code @MappedSuperclass} nên KHÔNG sinh ra bảng riêng: hai cột
 * {@code created_at} / {@code updated_at} được nhân bản vào từng bảng con.
 * Giá trị do Spring Data JPA Auditing tự điền (bật bằng {@code @EnableJpaAuditing}
 * ở lớp {@code TourBookingApplication}), nhờ vậy không phải nhớ gán tay ở service
 * và cũng không cần Trigger trong CSDL - điều mà đề bài không cho phép.</p>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class Auditable {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
