package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.tourbooking.config.AppProperties;
import vn.edu.iuh.fit.tourbooking.dto.form.TourForm;
import vn.edu.iuh.fit.tourbooking.dto.form.TourSearchForm;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;
import vn.edu.iuh.fit.tourbooking.entity.TourImage;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.tourbooking.repository.BookingDetailRepository;
import vn.edu.iuh.fit.tourbooking.repository.ContactMessageRepository;
import vn.edu.iuh.fit.tourbooking.repository.ReviewRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourCategoryRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourDepartureRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourImageRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.repository.spec.TourSpecifications;
import vn.edu.iuh.fit.tourbooking.util.SlugUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
// Nghiệp vụ tour: tìm kiếm/lọc công khai và thêm/sửa/xoá/quản lý ảnh cho khu quản trị.
public class TourService {

    public static final int PAGE_SIZE = 9;

    public static final int ADMIN_PAGE_SIZE = 15;

    private static final String TOUR_IMAGE_DIR = "tours";

    private final TourRepository tourRepository;
    private final TourDepartureRepository departureRepository;
    private final TourCategoryRepository categoryRepository;
    private final TourImageRepository tourImageRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ReviewRepository reviewRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final FileStorageService fileStorageService;
    private final AppProperties appProperties;

    public static final int MAX_API_PAGE_SIZE = 50;

    public static final int MAX_SUGGESTIONS = 8;

    @Transactional(readOnly = true)
    public Page<Tour> search(TourSearchForm form, int page) {
        return search(form, page, PAGE_SIZE);
    }

    @Transactional(readOnly = true)
    public Page<Tour> search(TourSearchForm form, int page, int size) {
        int safeSize = Math.clamp(size, 1, MAX_API_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, form.toSort());
        return tourRepository.findAll(TourSpecifications.from(form), pageable);
    }

    @Transactional(readOnly = true)
    // Gợi ý tour theo từ khoá đã bỏ dấu/chữ thường (khớp collation không phân biệt Đ/D).
    public List<Tour> suggest(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String normalized = SlugUtil.removeDiacritics(keyword.trim()).toLowerCase(Locale.ROOT);
        return tourRepository.suggest(normalized,
                PageRequest.of(0, Math.clamp(limit, 1, MAX_SUGGESTIONS)));
    }

    @Transactional(readOnly = true)
    // Chi tiết tour cho trang công khai; tour ngừng bán coi như không tồn tại (404).
    public Tour findDetail(Long id) {
        Tour tour = tourRepository.findDetailById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", id));

        if (!tour.isActive()) {
            throw ResourceNotFoundException.of("tour", id);
        }
        return tour;
    }

    @Transactional(readOnly = true)
    public void requireExists(Long id) {
        if (!tourRepository.existsById(id)) {
            throw ResourceNotFoundException.of("tour", id);
        }
    }

    @Transactional(readOnly = true)
    public List<Tour> findFeatured(int limit) {
        return tourRepository.findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc(
                PageRequest.of(0, limit));
    }

    @Transactional
    public Tour getDetail(Long id) {
        tourRepository.incrementViewCount(id);
        return tourRepository.findDetailById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", id));
    }

    @Transactional(readOnly = true)
    public List<TourDeparture> findBookableDepartures(Long tourId) {
        return departureRepository.findBookable(tourId,
                LocalDate.now().plusDays(appProperties.booking().cutoffDays()));
    }

    @Transactional(readOnly = true)
    public List<String> findAllDestinations() {
        return tourRepository.findAllDestinations();
    }

    public record SiteStats(long tourCount, long destinationCount,
                            long departureCount, long categoryCount) {
    }

    @Transactional(readOnly = true)
    public SiteStats siteStats() {
        return new SiteStats(
                tourRepository.count(),
                tourRepository.findAllDestinations().size(),
                departureRepository.count(),
                categoryRepository.count());
    }

    @Transactional(readOnly = true)
    public Page<Tour> adminSearch(String keyword, Long categoryId, int page) {

        String normalized = (keyword == null || keyword.isBlank())
                ? null
                : SlugUtil.removeDiacritics(keyword.trim()).toLowerCase(Locale.ROOT);

        return tourRepository.adminSearch(normalized, categoryId,
                PageRequest.of(Math.max(page, 0), ADMIN_PAGE_SIZE,
                        Sort.by(Sort.Direction.DESC, "id")));
    }

    // Tạo mới hoặc cập nhật tour: chặn trùng mã, tự sinh slug, thay ảnh đại diện và thêm ảnh vào thư viện.
    @Transactional
    public Tour save(TourForm form) {
        boolean creating = form.getId() == null;

        Tour tour = creating
                ? new Tour()
                : tourRepository.findById(form.getId())
                        .orElseThrow(() -> ResourceNotFoundException.of("tour", form.getId()));

        requireUniqueCode(form.getCode(), form.getId());

        TourCategory category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("danh mục", form.getCategoryId()));

        tour.setCode(form.getCode().trim());
        tour.setName(form.getName().trim());

        tour.setSlug(SlugUtil.toUniqueSlug(form.getName(), slug -> creating
                ? tourRepository.existsBySlug(slug)
                : tourRepository.existsBySlugAndIdNot(slug, form.getId())));
        tour.setShortDescription(form.getShortDescription());
        tour.setDescription(form.getDescription());
        tour.setItinerary(form.getItinerary());
        tour.setDepartureLocation(form.getDepartureLocation().trim());
        tour.setDestination(form.getDestination().trim());
        tour.setDurationDays(form.getDurationDays());
        tour.setDurationNights(form.getDurationNights());
        tour.setBasePrice(form.getBasePrice());
        tour.setTransportation(form.getTransportation());
        tour.setFeatured(form.isFeatured());
        tour.setActive(form.isActive());
        tour.setCategory(category);

        MultipartFile thumbnail = form.getThumbnailFile();
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String oldPath = tour.getThumbnail();
            tour.setThumbnail(fileStorageService.store(thumbnail, TOUR_IMAGE_DIR));
            fileStorageService.deleteAfterCommit(oldPath);
        }

        if (form.getGalleryFiles() != null) {
            int order = tour.getImages().size();
            for (MultipartFile file : form.getGalleryFiles()) {
                if (file != null && !file.isEmpty()) {
                    String path = fileStorageService.store(file, TOUR_IMAGE_DIR);
                    tour.addImage(new TourImage(path, null, order++));
                }
            }
        }

        Tour saved = tourRepository.save(tour);
        log.info("{} tour {} ({})", creating ? "Đã thêm" : "Đã cập nhật",
                saved.getCode(), saved.getName());
        return saved;
    }

    // Xoá tour cùng mọi đợt khởi hành và file ảnh; chặn nếu đã có lượt đặt/đánh giá/liên hệ.
    @Transactional
    public void delete(Long id) {
        long inBooking = bookingDetailRepository.countByDeparture_Tour_Id(id);
        if (inBooking > 0) {
            throw new BusinessRuleException("error.tour.delete.inBooking", inBooking);
        }

        long reviewCount = reviewRepository.countByTourId(id);
        if (reviewCount > 0) {
            throw new BusinessRuleException("error.tour.delete.hasReviews", reviewCount);
        }

        long contactCount = contactMessageRepository.countByTourId(id);
        if (contactCount > 0) {
            throw new BusinessRuleException("error.tour.delete.hasContactMessages", contactCount);
        }

        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", id));

        List<String> imagePaths = new ArrayList<>(tourImageRepository.findImagePathsByTourId(id));
        if (tour.getThumbnail() != null) {
            imagePaths.add(tour.getThumbnail());
        }

        departureRepository.deleteAll(tour.getDepartures());

        tourRepository.delete(tour);

        imagePaths.forEach(fileStorageService::deleteAfterCommit);
        log.info("Đã xoá tour {} cùng {} file ảnh", tour.getCode(), imagePaths.size());
    }

    @Transactional
    public boolean toggleActive(Long id) {
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", id));
        tour.setActive(!tour.isActive());
        return tour.isActive();
    }

    // Xoá một ảnh khỏi thư viện ảnh của tour, kể cả file vật lý.
    @Transactional
    public void deleteImage(Long tourId, Long imageId) {
        Tour tour = tourRepository.findDetailById(tourId)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", tourId));

        TourImage image = tour.getImages().stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("ảnh", imageId));

        String path = image.getImagePath();

        tour.removeImage(image);
        fileStorageService.deleteAfterCommit(path);
    }

    @Transactional(readOnly = true)
    public Tour getForEdit(Long id) {
        return tourRepository.findDetailById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", id));
    }

    public record TourAdminDetail(Tour tour, List<TourDeparture> departures,
                                  long reviewCount, Double avgRating, long orderCount) {
    }

    @Transactional(readOnly = true)
    public TourAdminDetail adminDetail(Long id) {
        Tour tour = getForEdit(id);
        List<TourDeparture> departures = departureRepository.findByTourIdOrderByDepartureDateAsc(id);
        long reviewCount = reviewRepository.countByTourId(id);
        Double avgRating = reviewRepository.averageRatingByTourId(id);
        long orderCount = bookingDetailRepository.countDistinctBookingsByTourId(id);
        return new TourAdminDetail(tour, departures, reviewCount, avgRating, orderCount);
    }

    private void requireUniqueCode(String code, Long excludeId) {
        boolean duplicated = excludeId == null
                ? tourRepository.existsByCode(code)
                : tourRepository.existsByCodeAndIdNot(code, excludeId);
        if (duplicated) {
            throw new BusinessRuleException("error.tour.codeExists", code);
        }
    }
}
