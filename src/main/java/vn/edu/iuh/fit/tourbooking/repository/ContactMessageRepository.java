package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.ContactMessage;
import vn.edu.iuh.fit.tourbooking.entity.ContactStatus;

@Repository
// Truy vấn liên hệ gửi từ biểu mẫu công khai, lọc theo trạng thái cho khu quản trị.
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    @EntityGraph(attributePaths = {"tour", "handledBy"})
    Page<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"tour", "handledBy"})
    Page<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactStatus status, Pageable pageable);

    long countByStatus(ContactStatus status);

    long countByTourId(Long tourId);

    long countByHandledById(Long userId);
}
