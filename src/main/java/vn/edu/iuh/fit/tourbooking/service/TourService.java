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

/**
 * Nghiệp vụ về tour: phần đọc cho trang công khai và phần thêm/sửa/xoá cho khu
 * vực quản trị.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TourService {

    /** Số tour trên một trang danh sách - chia hết cho 3 nên lưới Bootstrap không bị lẻ. */
    public static final int PAGE_SIZE = 9;

    /** Bảng ở màn quản trị hiển thị dạng danh sách nên để nhiều dòng hơn. */
    public static final int ADMIN_PAGE_SIZE = 15;

    /** Thư mục con trong thư mục upload dành cho ảnh tour. */
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

    /** Số tour tối đa một lần gọi web service được lấy - chặn kiểu xin {@code size=100000}. */
    public static final int MAX_API_PAGE_SIZE = 50;

    /** Số dòng gợi ý tối đa cho ô tìm kiếm. */
    public static final int MAX_SUGGESTIONS = 8;

    /** Danh sách tour có lọc, sắp xếp và phân trang. */
    @Transactional(readOnly = true)
    public Page<Tour> search(TourSearchForm form, int page) {
        return search(form, page, PAGE_SIZE);
    }

    /**
     * Cùng phép tìm kiếm nhưng cho phép chọn số dòng mỗi trang - web service cần
     * điều này vì lưới ba cột của giao diện web không liên quan gì tới bên gọi API.
     *
     * <p>Kích thước trang bị kẹp trong khoảng {@code 1..MAX_API_PAGE_SIZE}: để
     * người gọi tự do đặt {@code size} là mở đường cho một yêu cầu duy nhất kéo
     * cả bảng ra khỏi CSDL.</p>
     */
    @Transactional(readOnly = true)
    public Page<Tour> search(TourSearchForm form, int page, int size) {
        int safeSize = Math.clamp(size, 1, MAX_API_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, form.toSort());
        return tourRepository.findAll(TourSpecifications.from(form), pageable);
    }

    /**
     * Gợi ý cho ô tìm kiếm.
     *
     * <p>Từ khoá được bỏ dấu và hạ chữ thường trước khi truyền xuống, vì truy vấn
     * chạy trên cột {@code tours.search_text} vốn lưu ở dạng đã bỏ dấu.</p>
     */
    @Transactional(readOnly = true)
    public List<Tour> suggest(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String normalized = SlugUtil.removeDiacritics(keyword.trim()).toLowerCase(Locale.ROOT);
        return tourRepository.suggest(normalized,
                PageRequest.of(0, Math.clamp(limit, 1, MAX_SUGGESTIONS)));
    }

    /**
     * Nạp tour cho web service - <b>không</b> tăng lượt xem.
     *
     * <p>Khác {@link #getDetail(Long)} đúng ở điểm đó. Lượt xem là để đo người
     * thật mở trang, còn web service thì ô chọn ngày gọi lại mỗi lần người dùng
     * đổi lựa chọn; đếm cả những lần ấy sẽ làm hỏng bảng xếp hạng "xem nhiều nhất".</p>
     */
    @Transactional(readOnly = true)
    public Tour findDetail(Long id) {
        Tour tour = tourRepository.findDetailById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", id));
        // Nhất quán với trang công khai /tours/{id} (xem TourController dòng
        // "!tour.isActive() && !isAdmin"): tour ngừng bán không còn được xem là
        // "tồn tại" đối với khách vãng lai, cả ở trang web lẫn ở web service. Trước
        // bản vá này, GET /api/tours/{id} vẫn trả 200 kèm đầy đủ dữ liệu của một
        // tour đã ngừng bán trong khi trang web cùng id lại trả 404 - hai đường vào
        // cùng một dữ liệu cho hai câu trả lời khác nhau.
        if (!tour.isActive()) {
            throw ResourceNotFoundException.of("tour", id);
        }
        return tour;
    }

    /** Ném {@link ResourceNotFoundException} nếu không có tour nào mang mã số này. */
    @Transactional(readOnly = true)
    public void requireExists(Long id) {
        if (!tourRepository.existsById(id)) {
            throw ResourceNotFoundException.of("tour", id);
        }
    }

    /** Tour nổi bật cho trang chủ. */
    @Transactional(readOnly = true)
    public List<Tour> findFeatured(int limit) {
        return tourRepository.findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc(
                PageRequest.of(0, limit));
    }

    /**
     * Nạp một tour cho trang chi tiết, kèm danh mục và bộ ảnh.
     *
     * <p>Tăng lượt xem <b>trước</b> khi nạp, vì câu {@code UPDATE} có
     * {@code clearAutomatically = true}: nó xoá sạch ngữ cảnh lưu trữ, nếu nạp
     * trước thì đối tượng vừa lấy ra sẽ bị tách khỏi ngữ cảnh và các quan hệ nạp
     * kèm coi như mất công.</p>
     */
    @Transactional
    public Tour getDetail(Long id) {
        tourRepository.incrementViewCount(id);
        return tourRepository.findDetailById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", id));
    }

    /**
     * Các đợt khởi hành khách còn đặt được của một tour.
     *
     * <p>Nạp riêng chứ không {@code join fetch} chung với bộ ảnh ở
     * {@code findDetailById}: lấy đồng thời hai collection trong một câu truy vấn
     * sẽ tạo tích Descartes, số dòng trả về bằng tích số ảnh nhân số đợt.</p>
     *
     * <p>Đây là <b>một trong bốn chỗ</b> áp hạn chót đặt tour (mục 12.5): trang
     * chi tiết, web service {@code /api/tours/{id}/departures} và trang danh sách
     * đều đi qua đúng phương thức này, nên chỉ cần một chỗ để đổi số ngày hạn
     * chót. Hai chỗ còn lại (thêm giỏ, thanh toán) gọi thẳng
     * {@code TourDeparture.isBookable(cutoffDays)} vì chúng làm việc với MỘT đợt
     * khởi hành cụ thể chứ không phải danh sách.</p>
     */
    @Transactional(readOnly = true)
    public List<TourDeparture> findBookableDepartures(Long tourId) {
        return departureRepository.findBookable(tourId,
                LocalDate.now().plusDays(appProperties.booking().cutoffDays()));
    }

    /** Danh sách điểm đến để đổ vào ô lọc. */
    @Transactional(readOnly = true)
    public List<String> findAllDestinations() {
        return tourRepository.findAllDestinations();
    }

    /**
     * Bốn con số hiện ở khối giới thiệu trang chủ.
     *
     * @param tourCount        số tour đang bán
     * @param destinationCount số điểm đến khác nhau
     * @param departureCount   số đợt khởi hành đã mở
     * @param categoryCount    số danh mục
     */
    public record SiteStats(long tourCount, long destinationCount,
                            long departureCount, long categoryCount) {
    }

    /**
     * Số liệu thật lấy thẳng từ cơ sở dữ liệu.
     *
     * <p>Cố ý <b>không</b> viết cứng những con số đẹp vào khuôn mẫu: quản trị viên
     * thêm một tour là trang chủ phải đổi theo, và khi vấn đáp thì mở SSMS đếm
     * cũng ra đúng con số đang hiện trên màn hình.</p>
     */
    @Transactional(readOnly = true)
    public SiteStats siteStats() {
        return new SiteStats(
                tourRepository.count(),
                tourRepository.findAllDestinations().size(),
                departureRepository.count(),
                categoryRepository.count());
    }

    // =====================================================================
    //  Phần dành cho khu vực quản trị
    // =====================================================================

    /** Danh sách tour ở màn quản trị - có cả tour đã ngừng bán. */
    @Transactional(readOnly = true)
    public Page<Tour> adminSearch(String keyword, Long categoryId, int page) {
        // Chuẩn hoá giống hệt trang công khai vì cũng tìm trên cột searchText.
        String normalized = (keyword == null || keyword.isBlank())
                ? null
                : SlugUtil.removeDiacritics(keyword.trim()).toLowerCase(Locale.ROOT);

        return tourRepository.adminSearch(normalized, categoryId,
                PageRequest.of(Math.max(page, 0), ADMIN_PAGE_SIZE,
                        Sort.by(Sort.Direction.DESC, "id")));
    }

    /**
     * Thêm mới hoặc cập nhật một tour, kèm xử lý ảnh.
     *
     * @return tour đã lưu
     */
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
        // Slug sinh lại theo tên mới, có hậu tố -2, -3... nếu trùng.
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

        // Ảnh đại diện: để trống nghĩa là giữ nguyên ảnh cũ.
        MultipartFile thumbnail = form.getThumbnailFile();
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String oldPath = tour.getThumbnail();
            tour.setThumbnail(fileStorageService.store(thumbnail, TOUR_IMAGE_DIR));
            fileStorageService.deleteAfterCommit(oldPath);
        }

        // Thư viện ảnh: mỗi lần lưu chỉ thêm vào, muốn bỏ ảnh nào thì xoá riêng.
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

    /**
     * Xoá một tour.
     *
     * <p><b>Đây là chỗ hiện thực yêu cầu "không cho xoá dữ liệu đang được tham
     * chiếu" của đề bài.</b> Phép kiểm tra nằm ở đây, trong chương trình, chứ
     * không nhờ ràng buộc khoá ngoại của CSDL: khoá ngoại chỉ ném ra một lỗi SQL
     * khó hiểu, còn ở đây ta báo được cho người dùng biết vì sao không xoá được
     * và gợi ý cách khác (ngừng bán thay vì xoá).</p>
     */
    @Transactional
    public void delete(Long id) {
        long inBooking = bookingDetailRepository.countByDeparture_Tour_Id(id);
        if (inBooking > 0) {
            throw new BusinessRuleException("error.tour.delete.inBooking", inBooking);
        }

        // Hai phép kiểm dưới đây sinh ra cùng lúc với bảng reviews và
        // contact_messages: cả hai đều trỏ tới tours, nên thiếu chúng thì lỗi
        // khoá ngoại sẽ nổ ra dưới dạng trang 500 thay vì một câu tiếng Việt.
        // Lịch trình từng ngày thì ngược lại - nó thuộc sở hữu của tour
        // (cascade ALL + orphanRemoval) nên bị xoá theo, không chặn.
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

        // Gom đường dẫn ảnh TRƯỚC khi xoá bản ghi, vì sau đó không đọc được nữa.
        List<String> imagePaths = new ArrayList<>(tourImageRepository.findImagePathsByTourId(id));
        if (tour.getThumbnail() != null) {
            imagePaths.add(tour.getThumbnail());
        }

        // Quan hệ Tour -> TourDeparture cố ý KHÔNG có CascadeType.REMOVE (để lỡ
        // tay xoá thì thất bại ở khoá ngoại chứ không âm thầm xoá đợt đã có khách).
        // Tới đây đã chắc chắn không đợt nào có khách đặt, nên xoá tường minh.
        departureRepository.deleteAll(tour.getDepartures());

        tourRepository.delete(tour);

        // File ảnh chỉ bị xoá sau khi giao dịch commit thành công.
        imagePaths.forEach(fileStorageService::deleteAfterCommit);
        log.info("Đã xoá tour {} cùng {} file ảnh", tour.getCode(), imagePaths.size());
    }

    /** Ngừng bán / bán lại - phương án thay thế khi không xoá được. */
    @Transactional
    public boolean toggleActive(Long id) {
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", id));
        tour.setActive(!tour.isActive());
        return tour.isActive();
    }

    /** Xoá một ảnh khỏi thư viện ảnh của tour. */
    @Transactional
    public void deleteImage(Long tourId, Long imageId) {
        Tour tour = tourRepository.findDetailById(tourId)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", tourId));

        TourImage image = tour.getImages().stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("ảnh", imageId));

        String path = image.getImagePath();
        // orphanRemoval = true nên gỡ khỏi danh sách là bản ghi bị xoá theo.
        tour.removeImage(image);
        fileStorageService.deleteAfterCommit(path);
    }

    /** Nạp tour để đổ vào biểu mẫu sửa, kèm danh mục và thư viện ảnh. */
    @Transactional(readOnly = true)
    public Tour getForEdit(Long id) {
        return tourRepository.findDetailById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("tour", id));
    }

    /**
     * Gói dữ liệu cho trang chi tiết CHỈ XEM của quản trị viên (mục 12.8 - đề
     * bài đòi "xem chi tiết từng tour"). Tách khỏi biểu mẫu sửa: trang này không
     * có ô nhập nào, chỉ đọc.
     */
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
