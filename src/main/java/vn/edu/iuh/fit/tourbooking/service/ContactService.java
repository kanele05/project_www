package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.dto.form.ContactForm;
import vn.edu.iuh.fit.tourbooking.entity.ContactMessage;
import vn.edu.iuh.fit.tourbooking.entity.ContactStatus;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.tourbooking.repository.ContactMessageRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;

import java.time.LocalDateTime;

/**
 * Nghiệp vụ hộp thư liên hệ (UC021 gửi công khai, UC022 quản trị viên xử lý).
 *
 * <p>Bảng {@code contact_messages} tồn tại từ Phase 7 nhưng chưa từng được ghi
 * vào - đây là chỗ vá lại, đúng thiết kế chốt ở
 * {@code docs/report/SPEC_CHUNG.md} mục 9. Không có quy tắc nào cần kiểm ở tầng
 * Java khi <b>gửi</b> (ai cũng gửi được, kể cả spam - lọc bằng {@code SPAM} ở
 * khâu quản trị chứ không chặn lúc gửi).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    public static final int ADMIN_PAGE_SIZE = 15;

    private final ContactMessageRepository contactMessageRepository;
    private final TourRepository tourRepository;

    /** Tour đang hỏi (nếu có) để điền sẵn phần "Đang hỏi về tour này" trên biểu mẫu. */
    @Transactional(readOnly = true)
    public Tour findTourForPrefill(Long tourId) {
        if (tourId == null) {
            return null;
        }
        return tourRepository.findById(tourId).orElse(null);
    }

    @Transactional
    public ContactMessage submit(ContactForm form) {
        ContactMessage message = new ContactMessage(
                form.getFullName().trim(), form.getEmail().trim(),
                form.getSubject().trim(), form.getContent().trim());
        message.setPhone(form.getPhone() == null || form.getPhone().isBlank()
                ? null : form.getPhone().trim());

        if (form.getTourId() != null) {
            tourRepository.findById(form.getTourId()).ifPresent(message::setTour);
        }

        ContactMessage saved = contactMessageRepository.save(message);
        log.info("Liên hệ mới từ {} <{}> - chủ đề: {}",
                saved.getFullName(), saved.getEmail(), saved.getSubject());
        return saved;
    }

    // =====================================================================
    //  Phần dành cho khu vực quản trị (UC022)
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<ContactMessage> adminList(ContactStatus status, int page) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), ADMIN_PAGE_SIZE);
        return status == null
                ? contactMessageRepository.findAllByOrderByCreatedAtDesc(pageable)
                : contactMessageRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    /** Huy hiệu đếm số liên hệ mới trên menu quản trị. */
    @Transactional(readOnly = true)
    public long countNew() {
        return contactMessageRepository.countByStatus(ContactStatus.NEW);
    }

    @Transactional(readOnly = true)
    public ContactMessage getById(Long id) {
        return contactMessageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("liên hệ", id));
    }

    /**
     * Đổi trạng thái một liên hệ, kèm ghi chú trả lời.
     *
     * <p>{@code handledBy}/{@code handledAt} chỉ đặt khi chuyển sang
     * {@code RESOLVED} hoặc {@code SPAM} - đúng yêu cầu UC022: đó là hai trạng
     * thái coi như "đã xong việc", còn {@code IN_PROGRESS} chỉ là đang xử lý dở.</p>
     */
    @Transactional
    public void updateStatus(Long id, ContactStatus newStatus, User staff, String replyNote) {
        ContactMessage message = getById(id);
        message.setStatus(newStatus);

        if (replyNote != null && !replyNote.isBlank()) {
            message.setReplyNote(replyNote.trim());
        }

        if (newStatus == ContactStatus.RESOLVED || newStatus == ContactStatus.SPAM) {
            message.setHandledBy(staff);
            message.setHandledAt(LocalDateTime.now());
        }

        log.info("Liên hệ id={} đổi trạng thái -> {} (bởi {})",
                id, newStatus, staff == null ? "?" : staff.getEmail());
    }
}
