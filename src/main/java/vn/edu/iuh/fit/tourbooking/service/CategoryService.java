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

@Service
@RequiredArgsConstructor
@Slf4j
// Nghiệp vụ danh mục tour: phần đọc cho trang công khai, phần thêm/sửa/xoá cho khu quản trị.
public class CategoryService {

    public static final int ADMIN_PAGE_SIZE = 15;

    private static final String CATEGORY_IMAGE_DIR = "categories";

    private final TourCategoryRepository categoryRepository;
    private final TourRepository tourRepository;
    private final FileStorageService fileStorageService;

    public record CategoryCard(TourCategory category, long tourCount) {
    }

    @Transactional(readOnly = true)
    public List<TourCategory> findActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    // Danh mục đang bật kèm số tour mỗi danh mục, sắp theo số tour giảm dần.
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

    @Transactional(readOnly = true)
    public TourCategory getBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.of("danh mục", slug));
    }

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

    public record CategoryAdminDetail(TourCategory category, List<vn.edu.iuh.fit.tourbooking.entity.Tour> tours) {
    }

    @Transactional(readOnly = true)
    public CategoryAdminDetail adminDetail(Long id) {
        TourCategory category = getById(id);
        return new CategoryAdminDetail(category, tourRepository.findByCategoryIdOrderByNameAsc(id));
    }

    // Tạo mới hoặc cập nhật danh mục: chặn trùng tên, tự sinh slug, thay ảnh (xoá ảnh cũ nếu là ảnh tải lên).
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

            if (oldPath != null && !oldPath.startsWith("/")) {
                fileStorageService.deleteAfterCommit(oldPath);
            }
        }

        TourCategory saved = categoryRepository.save(category);
        log.info("{} danh mục {}", creating ? "Đã thêm" : "Đã cập nhật", saved.getName());
        return saved;
    }

    // Xoá danh mục; chặn nếu còn tour, xoá luôn ảnh nếu là ảnh tải lên.
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

    @Transactional
    public boolean toggleActive(Long id) {
        TourCategory category = getById(id);
        category.setActive(!category.isActive());
        return category.isActive();
    }
}
