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

@Service
@RequiredArgsConstructor
@Slf4j
// Nghiệp vụ hộp thư liên hệ: gửi công khai (UC021) và quản trị viên xử lý (UC022). Gửi thì không chặn ai.
public class ContactService {

    public static final int ADMIN_PAGE_SIZE = 15;

    private final ContactMessageRepository contactMessageRepository;
    private final TourRepository tourRepository;

    @Transactional(readOnly = true)
    public Tour findTourForPrefill(Long tourId) {
        if (tourId == null) {
            return null;
        }
        return tourRepository.findById(tourId).orElse(null);
    }

    // Ghi một liên hệ mới, gắn tour nếu có chọn.
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

    @Transactional(readOnly = true)
    public Page<ContactMessage> adminList(ContactStatus status, int page) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), ADMIN_PAGE_SIZE);
        return status == null
                ? contactMessageRepository.findAllByOrderByCreatedAtDesc(pageable)
                : contactMessageRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    @Transactional(readOnly = true)
    public long countNew() {
        return contactMessageRepository.countByStatus(ContactStatus.NEW);
    }

    @Transactional(readOnly = true)
    public ContactMessage getById(Long id) {
        return contactMessageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("liên hệ", id));
    }

    // Đổi trạng thái một liên hệ; chuyển sang RESOLVED/SPAM thì ghi người xử lý và thời điểm.
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
