package vn.edu.iuh.fit.tourbooking.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.dto.form.PromotionForm;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.CouponUsage;
import vn.edu.iuh.fit.tourbooking.entity.Promotion;
import vn.edu.iuh.fit.tourbooking.entity.User;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.tourbooking.repository.CouponUsageRepository;
import vn.edu.iuh.fit.tourbooking.repository.PromotionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
// Nghiệp vụ mã khuyến mãi: kiểm mã lúc checkout, ghi/trả lượt dùng có khoá bi quan chống lost update.
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final EntityManager entityManager;

    public record CouponCheck(Promotion promotion, BigDecimal discount) {

        public String getCode() {
            return promotion.getCode();
        }

        public String getName() {
            return promotion.getName();
        }
    }

    // Kiểm một mã có dùng được không cho đơn hàng này: còn hiệu lực, còn lượt (chung và theo người), đủ đơn tối thiểu.
    @Transactional(readOnly = true)
    public CouponCheck check(String code, Long userId, BigDecimal orderAmount) {
        String normalized = code == null ? "" : code.trim();
        if (normalized.isEmpty()) {
            throw new BusinessRuleException("error.coupon.empty");
        }

        Promotion promotion = promotionRepository.findByCodeIgnoreCase(normalized)
                .orElseThrow(() -> new BusinessRuleException("error.coupon.notFound", normalized));

        if (!promotion.isRunning(LocalDateTime.now())) {

            if (!promotion.hasQuotaLeft()) {
                throw new BusinessRuleException("error.coupon.outOfQuota", normalized);
            }
            throw new BusinessRuleException("error.coupon.expired", normalized);
        }

        Integer perUser = promotion.getUsageLimitPerUser();
        if (perUser != null && userId != null) {
            long used = couponUsageRepository.countByPromotionIdAndUserId(promotion.getId(), userId);
            if (used >= perUser) {
                throw new BusinessRuleException("error.coupon.usedByUser", normalized, perUser);
            }
        }

        BigDecimal discount = promotion.calculateDiscount(orderAmount);
        if (discount.signum() <= 0) {

            throw new BusinessRuleException("error.coupon.minOrder",
                    normalized, promotion.getMinOrderAmount());
        }
        return new CouponCheck(promotion, discount);
    }

    // Ghi nhận một lượt dùng mã thật: khoá bi quan + refresh (bắt buộc, xem gotcha #60) rồi mới kiểm lại quota và tăng used_count.
    @Transactional
    public CouponUsage recordUsage(Promotion promotion, User user, Booking booking,
                                   BigDecimal discount) {
        Promotion locked = promotionRepository.findByIdForUpdate(promotion.getId())
                .orElseThrow(() -> new BusinessRuleException("error.coupon.notFound", promotion.getCode()));

        entityManager.refresh(locked);

        if (!locked.hasQuotaLeft()) {
            throw new BusinessRuleException("error.coupon.outOfQuota", locked.getCode());
        }
        Integer perUser = locked.getUsageLimitPerUser();
        if (perUser != null && user != null) {
            long used = couponUsageRepository.countByPromotionIdAndUserId(locked.getId(), user.getId());
            if (used >= perUser) {
                throw new BusinessRuleException("error.coupon.usedByUser", locked.getCode(), perUser);
            }
        }

        locked.increaseUsedCount();
        promotionRepository.save(locked);

        CouponUsage usage = new CouponUsage(locked, user, booking, discount);
        couponUsageRepository.save(usage);

        log.info("Đơn {} dùng mã {} - giảm {} đ", booking.getCode(), locked.getCode(), discount);
        return usage;
    }

    // Tính lại tiền giảm theo đúng luật của mã khi số khách/thành tiền đơn đổi, đồng bộ luôn coupon_usages.
    @Transactional
    public void recalculateDiscount(Booking booking) {
        Promotion promotion = booking.getPromotion();
        if (promotion == null) {
            return;
        }
        BigDecimal newDiscount = promotion.calculateDiscount(booking.getSubtotalAmount());
        booking.setDiscountAmount(newDiscount);

        couponUsageRepository.findByBookingId(booking.getId())
                .ifPresent(usage -> usage.setDiscountAmount(newDiscount));

        log.info("Đơn {}: tính lại tiền giảm theo mã {} - còn {} đ",
                booking.getCode(), promotion.getCode(), newDiscount);
    }

    // Trả lại một lượt dùng mã khi đơn bị huỷ: khoá bi quan + refresh, giảm used_count, xoá dòng coupon_usages.
    @Transactional
    public void releaseUsage(Booking booking) {
        couponUsageRepository.findByBookingId(booking.getId()).ifPresent(usage -> {
            Promotion locked = promotionRepository.findByIdForUpdate(usage.getPromotion().getId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "error.coupon.notFound", usage.getPromotion().getCode()));
            entityManager.refresh(locked);

            locked.decreaseUsedCount();
            promotionRepository.save(locked);
            couponUsageRepository.delete(usage);

            log.info("Đơn {}: trả lại 1 lượt dùng mã {} (used_count còn {})",
                    booking.getCode(), locked.getCode(), locked.getUsedCount());
        });
    }

    public static final int ADMIN_PAGE_SIZE = 15;

    public static final int USAGE_PAGE_SIZE = 20;

    @Transactional(readOnly = true)
    public Page<Promotion> adminSearch(String keyword, Boolean active, int page) {
        return promotionRepository.search(keyword, active,
                PageRequest.of(Math.max(page, 0), ADMIN_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id")));
    }

    @Transactional(readOnly = true)
    public Promotion getById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("mã khuyến mãi", id));
    }

    @Transactional(readOnly = true)
    public Page<CouponUsage> usagesOf(Long promotionId, int page) {
        return couponUsageRepository.findByPromotionIdOrderByUsedAtDesc(promotionId,
                PageRequest.of(Math.max(page, 0), USAGE_PAGE_SIZE));
    }

    // Tạo mới hoặc cập nhật mã khuyến mãi; chặn trùng mã (không phân biệt hoa/thường).
    @Transactional
    public Promotion save(PromotionForm form) {
        boolean creating = form.getId() == null;
        String code = form.getCode().trim().toUpperCase(Locale.ROOT);

        boolean duplicated = creating
                ? promotionRepository.existsByCodeIgnoreCase(code)
                : promotionRepository.findByCodeIgnoreCase(code)
                        .map(p -> !p.getId().equals(form.getId()))
                        .orElse(false);
        if (duplicated) {
            throw new BusinessRuleException("error.promotion.codeExists", code);
        }

        Promotion promotion = creating ? new Promotion() : getById(form.getId());

        promotion.setCode(code);
        promotion.setName(form.getName().trim());
        promotion.setDescription(form.getDescription());
        promotion.setDiscountType(form.getDiscountType());
        promotion.setDiscountValue(form.getDiscountValue());
        promotion.setMaxDiscount(form.getMaxDiscount());
        promotion.setMinOrderAmount(form.getMinOrderAmount());
        promotion.setUsageLimit(form.getUsageLimit());
        promotion.setUsageLimitPerUser(form.getUsageLimitPerUser());
        promotion.setStartAt(form.getStartAt());
        promotion.setEndAt(form.getEndAt());
        promotion.setActive(form.isActive());

        Promotion saved = promotionRepository.save(promotion);
        log.info("{} mã khuyến mãi {}", creating ? "Đã thêm" : "Đã cập nhật", saved.getCode());
        return saved;
    }

    // Xoá mã khuyến mãi; chặn nếu mã đã có lượt dùng.
    @Transactional
    public void delete(Long id) {
        long usageCount = couponUsageRepository.countByPromotionId(id);
        if (usageCount > 0) {
            throw new BusinessRuleException("error.promotion.delete.hasUsages", usageCount);
        }
        Promotion promotion = getById(id);
        promotionRepository.delete(promotion);
        log.info("Đã xoá mã khuyến mãi {}", promotion.getCode());
    }

    @Transactional
    public boolean toggleActive(Long id) {
        Promotion promotion = getById(id);
        promotion.setActive(!promotion.isActive());
        return promotion.isActive();
    }
}
