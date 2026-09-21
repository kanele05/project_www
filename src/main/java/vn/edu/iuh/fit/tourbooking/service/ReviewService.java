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

/**
 * Nghiệp vụ đánh giá tour (UC018) và kiểm duyệt đánh giá (UC020).
 *
 * <p>Hai quy tắc kiểm khi <b>gửi</b> đánh giá, cả hai bằng Java chứ không có
 * ràng buộc tương ứng dưới CSDL:</p>
 * <ol>
 *   <li>chỉ khách có ít nhất một đơn {@code COMPLETED} chứa tour này mới được
 *       đánh giá - kiểm bằng {@link BookingDetailRepository#existsCompletedBookingForTour};</li>
 *   <li>mỗi tài khoản chỉ đánh giá một tour đúng một lần - kiểm bằng
 *       {@link ReviewRepository#existsByUserIdAndTourId}, không dùng chỉ mục
 *       duy nhất lọc của SQL Server (xem gotcha #41: chỉ mục có lọc buộc
 *       {@code QUOTED_IDENTIFIER ON}, làm gãy script seed chạy bằng sqlcmd).</li>
 * </ol>
 *
 * <p><b>UC020 (kiểm duyệt):</b> đánh giá gửi lên mang {@code approved = false} -
 * đúng thiết kế gốc của {@link Review} (xem Javadoc của trường {@code approved}).
 * Chỉ hiện ra trang công khai và chỉ tính vào điểm trung bình sau khi quản trị
 * viên duyệt ở {@code /admin/reviews}. Bộ lọc {@code approved = true} nằm ngay
 * trong các truy vấn {@link ReviewRepository#findByTourIdAndApprovedTrueOrderByCreatedAtDesc}
 * / {@link ReviewRepository#averageRatingByTourId}, không lọc ở template.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    /** Số đánh giá trên một trang ở khối đánh giá của trang chi tiết tour. */
    public static final int PAGE_SIZE = 5;

    /** Số dòng trên một trang ở màn kiểm duyệt của quản trị viên. */
    public static final int ADMIN_PAGE_SIZE = 15;

    /** Bộ lọc trạng thái ở màn quản trị {@code /admin/reviews}. */
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

    /**
     * Đánh giá của chính khách đang xem cho tour này, kể cả khi chưa duyệt -
     * trang chi tiết tour dùng để hiện nhãn "Chờ duyệt" cho chủ nhân đánh giá.
     */
    @Transactional(readOnly = true)
    public Optional<Review> findOwnReview(Long userId, Long tourId) {
        if (userId == null) {
            return Optional.empty();
        }
        return reviewRepository.findByUserIdAndTourId(userId, tourId);
    }

    /** Điểm trung bình, chỉ tính đánh giá đã duyệt; {@code null} nếu chưa có đánh giá nào. */
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

    /** Đủ điều kiện đánh giá: đã đi (đơn hoàn tất) và chưa từng đánh giá tour này. */
    @Transactional(readOnly = true)
    public boolean canReview(Long userId, Long tourId) {
        if (userId == null) {
            return false;
        }
        if (reviewRepository.existsByUserIdAndTourId(userId, tourId)) {
            return false;
        }
        return bookingDetailRepository.existsCompletedBookingForTour(tourId, userId);
    }

    /**
     * Ghi nhận một đánh giá mới.
     *
     * <p>Kiểm lại CẢ HAI điều kiện ở đây, không tin vào việc nút "Đánh giá" chỉ
     * hiện ra khi {@link #canReview} trả về true - biểu mẫu hoàn toàn có thể
     * được gửi thẳng bằng tay tới {@code POST /account/reviews}.</p>
     */
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
        // UC020: chờ quản trị viên duyệt ở /admin/reviews - đúng thiết kế gốc
        // của entity, xem Javadoc của Review.approved.
        review.setApproved(false);

        Review saved = reviewRepository.save(review);
        log.info("Tài khoản id={} gửi đánh giá tour {} - {} sao, chờ duyệt",
                userId, tour.getCode(), form.getRating());
        return saved;
    }

    // =====================================================================
    //  UC020 - Kiểm duyệt đánh giá, dành cho khu vực quản trị
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<Review> adminList(StatusFilter filter, int page) {
        // KHÔNG thêm Sort ở đây: ba phương thức repository bên dưới đã tự sắp
        // theo tên (...OrderByCreatedAtDesc). Cộng thêm Sort trùng cột vào
        // Pageable khiến Spring Data nối hai lượt sắp giống hệt nhau, ra
        // "ORDER BY created_at DESC, created_at DESC" - SQL Server từ chối với
        // Msg "A column has been specified more than once in the order by list"
        // (các hệ quản trị khác thường im lặng bỏ qua, nên lỗi này chỉ lộ ra ở
        // đây, không lộ ở H2/MySQL khi viết unit test).
        PageRequest pageable = PageRequest.of(Math.max(page, 0), ADMIN_PAGE_SIZE);
        return switch (filter) {
            case PENDING -> reviewRepository.findByApprovedFalseOrderByCreatedAtDesc(pageable);
            case APPROVED -> reviewRepository.findByApprovedTrueOrderByCreatedAtDesc(pageable);
            case ALL -> reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        };
    }

    /** Huy hiệu đếm số đánh giá đang chờ duyệt trên menu quản trị. */
    @Transactional(readOnly = true)
    public long countPending() {
        return reviewRepository.countByApprovedFalse();
    }

    @Transactional(readOnly = true)
    public Review getById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("đánh giá", id));
    }

    /** Duyệt: đánh giá bắt đầu hiện ra trang công khai và được tính vào điểm trung bình. */
    @Transactional
    public void approve(Long id) {
        Review review = getById(id);
        review.setApproved(true);
        log.info("Đã duyệt đánh giá id={} (tour {})", id, review.getTour().getCode());
    }

    /** Bỏ duyệt: gỡ khỏi trang công khai mà không xoá, quản trị viên có thể duyệt lại sau. */
    @Transactional
    public void unapprove(Long id) {
        Review review = getById(id);
        review.setApproved(false);
        log.info("Đã bỏ duyệt đánh giá id={} (tour {})", id, review.getTour().getCode());
    }

    /** Trả lời một đánh giá - dùng sẵn {@link Review#reply(String)}. */
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
