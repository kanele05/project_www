package vn.edu.iuh.fit.tourbooking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.tourbooking.entity.ContactMessage;
import vn.edu.iuh.fit.tourbooking.entity.ContactStatus;

/** Truy vấn hộp thư liên hệ. */
@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    /**
     * Hai truy vấn bên dưới phục vụ màn {@code /admin/contacts} (UC022). Nạp sẵn
     * {@code tour} và {@code handledBy} bằng {@code @EntityGraph}: bảng hiện tên
     * tour đang hỏi và người đã tiếp nhận ngay trên mỗi dòng, mà
     * {@code open-in-view} đang tắt nên chạm hai quan hệ LAZY đó lúc dựng khuôn
     * mẫu sẽ cắt cụt trang giữa chừng (gotcha #43).
     */
    @EntityGraph(attributePaths = {"tour", "handledBy"})
    Page<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"tour", "handledBy"})
    Page<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactStatus status, Pageable pageable);

    /** Số thư mới - hiện thành huy hiệu trên thanh điều hướng khu quản trị. */
    long countByStatus(ContactStatus status);

    /** Hai truy vấn đếm dưới đây phục vụ quy tắc chặn xoá tour và tài khoản. */
    long countByTourId(Long tourId);

    long countByHandledById(Long userId);
}
