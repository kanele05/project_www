package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.dto.form.ReviewForm;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.Review;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.tourbooking.repository.BookingDetailRepository;
import vn.edu.iuh.fit.tourbooking.repository.ReviewRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
// Nghiệp vụ đánh giá tour: gửi đánh giá (chỉ khách đã COMPLETED mới được) và kiểm duyệt ở khu quản trị.
public class ReviewService {

    public static final int PAGE_SIZE = 5;

    public static final int ADMIN_PAGE_SIZE = 15;

    public enum StatusFilter { PENDING, APPROVED, ALL }

    private final ReviewRepository reviewRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<Review> findApprovedByTour(Long tourId, int page) {
        return reviewRepository.findByTourIdAndApprovedTrueOrderByCreatedAtDesc(
                tourId, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public Optional<Review> findOwnReview(Long userId, Long tourId) {
        if (userId == null) {
            return Optional.empty();
        }
        return reviewRepository.findByUserIdAndTourId(userId, tourId);
    }

    @Transactional(readOnly = true)
    public Double averageRating(Long tourId) {
        return reviewRepository.averageRatingByTourId(tourId);
    }

    @Transactional(readOnly = true)
    public long countApproved(Long tourId) {
        return reviewRepository.countByTourIdAndApprovedTrue(tourId);
    }

    @Transactional(readOnly = true)
    public boolean hasReviewed(Long userId, Long tourId) {
        return userId != null && reviewRepository.existsByUserIdAndTourId(userId, tourId);
    }

    @Transactional(readOnly = true)
    // Đủ điều kiện đánh giá không: đã đăng nhập, chưa từng đánh giá, và có đơn COMPLETED chứa tour này.
    public boolean canReview(Long userId, Long tourId) {
        if (userId == null) {
            return false;
        }
        if (reviewRepository.existsByUserIdAndTourId(userId, tourId)) {
            return false;
        }
        return bookingDetailRepository.existsCompletedBookingForTour(tourId, userId);
    }

    // Ghi đánh giá mới, gắn đơn COMPLETED làm bằng chứng "đã xác thực", mặc định chưa duyệt.
    @Transactional
    public Review submit(Long userId, ReviewForm form) {
        Long tourId = form.getTourId();

        if (reviewRepository.existsByUserIdAndTourId(userId, tourId)) {
            throw new BusinessRuleException("error.review.alreadyReviewed");
        }

        List<Booking> completed =
                bookingDetailRepository.findCompletedBookingsForTour(tourId, userId);
        if (completed.isEmpty()) {
            throw new BusinessRuleException("error.review.notEligible");
        }

        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", tourId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("người dùng", userId));

        Review review = new Review(tour, user, form.getRating(), form.getContent().trim());
        review.setTitle(form.getTitle() == null || form.getTitle().isBlank()
                ? null : form.getTitle().trim());
        review.setBooking(completed.get(0));

        review.setApproved(false);

        Review saved = reviewRepository.save(review);
        log.info("Tài khoản id={} gửi đánh giá tour {} - {} sao, chờ duyệt",
                userId, tour.getCode(), form.getRating());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Review> adminList(StatusFilter filter, int page) {

        PageRequest pageable = PageRequest.of(Math.max(page, 0), ADMIN_PAGE_SIZE);
        return switch (filter) {
            case PENDING -> reviewRepository.findByApprovedFalseOrderByCreatedAtDesc(pageable);
            case APPROVED -> reviewRepository.findByApprovedTrueOrderByCreatedAtDesc(pageable);
            case ALL -> reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        };
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return reviewRepository.countByApprovedFalse();
    }

    @Transactional(readOnly = true)
    public Review getById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("đánh giá", id));
    }

    @Transactional
    public void approve(Long id) {
        Review review = getById(id);
        review.setApproved(true);
        log.info("Đã duyệt đánh giá id={} (tour {})", id, review.getTour().getCode());
    }

    @Transactional
    public void unapprove(Long id) {
        Review review = getById(id);
        review.setApproved(false);
        log.info("Đã bỏ duyệt đánh giá id={} (tour {})", id, review.getTour().getCode());
    }

    @Transactional
    public void reply(Long id, String replyText) {
        if (replyText == null || replyText.isBlank()) {
            throw new BusinessRuleException("error.review.reply.empty");
        }
        Review review = getById(id);
        review.reply(replyText.trim());
        log.info("Đã trả lời đánh giá id={} (tour {})", id, review.getTour().getCode());
    }

    @Transactional
    public void delete(Long id) {
        Review review = getById(id);
        reviewRepository.delete(review);
        log.info("Đã xoá đánh giá id={} (tour {})", id, review.getTour().getCode());
    }
}
