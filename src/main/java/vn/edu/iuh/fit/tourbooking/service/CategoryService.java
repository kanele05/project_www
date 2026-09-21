package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.tourbooking.dto.form.CategoryForm;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.tourbooking.repository.TourCategoryRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.util.SlugUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ về danh mục tour: phần đọc cho trang công khai và phần thêm/sửa/xoá
 * cho khu vực quản trị.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    public static final int ADMIN_PAGE_SIZE = 15;

    /** Thư mục con trong thư mục upload dành cho ảnh danh mục. */
    private static final String CATEGORY_IMAGE_DIR = "categories";

    private final TourCategoryRepository categoryRepository;
    private final TourRepository tourRepository;
    private final FileStorageService fileStorageService;

    /** Một ô danh mục ở trang chủ: danh mục kèm số tour đang bán của nó. */
    public record CategoryCard(TourCategory category, long tourCount) {
    }

    /** Danh mục hiện trên thanh điều hướng - chỉ lấy danh mục đang bật. */
    @Transactional(readOnly = true)
    public List<TourCategory> findActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc();
    }

    /**
     * Danh mục kèm số tour, xếp danh mục nhiều tour nhất lên đầu.
     *
     * <p>Xếp theo số tour là có chủ đích: ô đầu tiên trên lưới trang chủ chiếm
     * gấp bốn diện tích các ô còn lại, nên nó phải là danh mục có nhiều thứ để
     * xem nhất chứ không phải danh mục tình cờ đứng đầu bảng chữ cái.</p>
     */
    @Transactional(readOnly = true)
    public List<CategoryCard> findActiveWithTourCount() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : tourRepository.countActiveGroupedByCategory()) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        return findActiveCategories().stream()
                .map(c -> new CategoryCard(c, counts.getOrDefault(c.getId(), 0L)))
                .sorted(Comparator.comparingLong(CategoryCard::tourCount).reversed())
                .toList();
    }

    /**
     * Tra danh mục theo slug cho địa chỉ {@code /categories/{slug}}.
     *
     * <p>Dùng slug thay vì mã số để địa chỉ vừa dễ đọc vừa có ích cho tìm kiếm,
     * ví dụ {@code /categories/du-lich-bien-dao}.</p>
     */
    @Transactional(readOnly = true)
    public TourCategory getBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.of("danh mục", slug));
    }

    // =====================================================================
    //  Phần dành cho khu vực quản trị
    // =====================================================================

    /** Toàn bộ danh mục, kể cả danh mục đang ẩn. */
    @Transactional(readOnly = true)
    public List<TourCategory> findAll() {
        return categoryRepository.findAll(Sort.by("name"));
    }

    @Transactional(readOnly = true)
    public Page<TourCategory> adminSearch(String keyword, int page) {
        return categoryRepository.search(keyword,
                PageRequest.of(Math.max(page, 0), ADMIN_PAGE_SIZE, Sort.by("name")));
    }

    @Transactional(readOnly = true)
    public TourCategory getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("danh mục", id));
    }

    @Transactional
    public TourCategory save(CategoryForm form) {
        boolean creating = form.getId() == null;

        boolean duplicatedName = creating
                ? categoryRepository.existsByName(form.getName())
                : categoryRepository.existsByNameAndIdNot(form.getName(), form.getId());
        if (duplicatedName) {
            throw new BusinessRuleException("error.category.nameExists", form.getName());
        }

        TourCategory category = creating ? new TourCategory() : getById(form.getId());

        category.setName(form.getName().trim());
        category.setSlug(SlugUtil.toUniqueSlug(form.getName(), slug -> creating
                ? categoryRepository.existsBySlug(slug)
                : categoryRepository.existsBySlugAndIdNot(slug, form.getId())));
        category.setDescription(form.getDescription());
        category.setActive(form.isActive());

        MultipartFile image = form.getImageFile();
        if (image != null && !image.isEmpty()) {
            String oldPath = category.getImageUrl();
            category.setImageUrl(fileStorageService.store(image, CATEGORY_IMAGE_DIR));
            // Ảnh danh mục của dữ liệu mẫu là đường dẫn tĩnh trong static/, không
            // phải file upload - đừng đụng tới chúng.
            if (oldPath != null && !oldPath.startsWith("/")) {
                fileStorageService.deleteAfterCommit(oldPath);
            }
        }

        TourCategory saved = categoryRepository.save(category);
        log.info("{} danh mục {}", creating ? "Đã thêm" : "Đã cập nhật", saved.getName());
        return saved;
    }

    /**
     * Xoá một danh mục.
     *
     * <p>Quy tắc chặn xoá thứ nhất của đề bài: danh mục còn tour thì không xoá
     * được. Kiểm tra bằng {@code countByCategoryId} trong Java, và thông báo kèm
     * luôn số tour đang vướng để người dùng biết phải làm gì tiếp.</p>
     */
    @Transactional
    public void delete(Long id) {
        long tourCount = tourRepository.countByCategoryId(id);
        if (tourCount > 0) {
            throw new BusinessRuleException("error.category.delete.hasTours", tourCount);
        }
        TourCategory category = getById(id);
        String imageUrl = category.getImageUrl();

        categoryRepository.delete(category);

        if (imageUrl != null && !imageUrl.startsWith("/")) {
            fileStorageService.deleteAfterCommit(imageUrl);
        }
        log.info("Đã xoá danh mục {}", category.getName());
    }

    /** Ẩn / hiện danh mục - phương án thay thế khi không xoá được. */
    @Transactional
    public boolean toggleActive(Long id) {
        TourCategory category = getById(id);
        category.setActive(!category.isActive());
        return category.isActive();
    }
}
