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

/**
 * Kiểm và áp mã giảm giá.
 *
 * <p><b>Toàn bộ điều kiện đều kiểm bằng Java</b>, không có ràng buộc nào tương ứng
 * dưới CSDL - đúng yêu cầu "kiểm tra ở tầng Model" của đề bài. Năm điều kiện, theo
 * đúng thứ tự người dùng dễ hiểu nhất:</p>
 * <ol>
 *   <li>mã có tồn tại không;</li>
 *   <li>còn trong thời gian hiệu lực và còn bật không;</li>
 *   <li>tổng lượt phát còn không;</li>
 *   <li>tài khoản này đã dùng quá số lượt cho phép chưa;</li>
 *   <li>đơn đã đủ giá trị tối thiểu chưa.</li>
 * </ol>
 *
 * <p>Cùng một phép kiểm phục vụ hai chỗ: nút "Áp dụng" gọi bằng AJAX để xem trước,
 * và {@code BookingService.placeOrder} gọi lại lần nữa lúc ghi đơn. <b>Phải kiểm
 * lại ở bước ghi đơn</b> - giữa hai lần đó mã có thể đã hết lượt, và người dùng
 * hoàn toàn có thể gửi thẳng biểu mẫu mà không bấm nút xem trước.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final EntityManager entityManager;

    /**
     * Kết quả kiểm mã: chương trình khuyến mãi và số tiền được giảm cho đơn hàng
     * có giá trị đã cho.
     */
    public record CouponCheck(Promotion promotion, BigDecimal discount) {

        public String getCode() {
            return promotion.getCode();
        }

        public String getName() {
            return promotion.getName();
        }
    }

    /**
     * Kiểm mã và tính số tiền giảm.
     *
     * @param code        mã khách gõ (không phân biệt hoa thường, tự cắt khoảng trắng)
     * @param userId      tài khoản đang đặt, để đếm số lượt đã dùng
     * @param orderAmount tiền hàng trước khi giảm
     * @throws BusinessRuleException kèm khoá câu thông báo nếu mã không dùng được
     */
    @Transactional(readOnly = true)
    public CouponCheck check(String code, Long userId, BigDecimal orderAmount) {
        String normalized = code == null ? "" : code.trim();
        if (normalized.isEmpty()) {
            throw new BusinessRuleException("error.coupon.empty");
        }

        Promotion promotion = promotionRepository.findByCodeIgnoreCase(normalized)
                .orElseThrow(() -> new BusinessRuleException("error.coupon.notFound", normalized));

        if (!promotion.isRunning(LocalDateTime.now())) {
            // Tách riêng "hết lượt" khỏi "hết hạn": hai câu trả lời khác nhau hẳn
            // với người dùng, gộp làm một là kiểu thông báo khiến khách gọi tổng đài.
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
            // Tới đây chỉ còn một lý do: đơn chưa đạt giá trị tối thiểu.
            throw new BusinessRuleException("error.coupon.minOrder",
                    normalized, promotion.getMinOrderAmount());
        }
        return new CouponCheck(promotion, discount);
    }

    /**
     * Ghi nhận một lượt dùng mã cho đơn vừa tạo.
     *
     * <p>Gọi bên trong giao dịch của {@code BookingService.placeOrder}: nếu việc ghi
     * đơn bị huỷ thì lượt dùng cũng biến mất theo, không có chuyện mã bị trừ lượt
     * cho một đơn không tồn tại.</p>
     *
     * <p><b>Khoá bi quan + kiểm lại quota (thay vì tin thẳng {@code promotion} đã
     * đọc trước đó ở {@link #check}).</b> Trước bản vá này, hai đơn cùng dùng một
     * mã gần như cùng lúc (còn đúng 1 lượt) đều đọc "còn lượt" rồi cùng ghi
     * {@code used_count = cũ + 1} - mất một lượt cập nhật (lost update):
     * {@code coupon_usages} có 2 dòng nhưng {@code used_count} chỉ tăng 1. Khoá
     * {@code findByIdForUpdate} chặn giao dịch thứ hai đợi giao dịch thứ nhất
     * commit xong mới đọc quota mới nhất - cùng cơ chế cũng chặn luôn việc lách
     * {@code usageLimitPerUser} bằng hai request song song từ cùng một tài khoản,
     * vì phép đếm {@code countByPromotionIdAndUserId} bên dưới chỉ chạy sau khi
     * đã giữ khoá.</p>
     *
     * <p><b>Gotcha vấp phải khi viết bản vá này, để lại làm bằng chứng:</b> chỉ
     * khoá thôi CHƯA ĐỦ. {@code promotion} tham số đã được nạp trước đó trong
     * cùng giao dịch (ở {@code PromotionService.check}, gọi từ
     * {@code BookingService.placeOrder}) nên đã có mặt trong persistence context
     * theo đúng {@code id}. Khi giao dịch B bị khoá bi quan chặn lại rồi được mở
     * khoá sau khi giao dịch A commit, câu {@code SELECT ... FOR UPDATE} của B có
     * chạy thật và có lấy đúng {@code used_count} mới nhất từ CSDL, nhưng Hibernate
     * trả về <b>đúng đối tượng Java đã có sẵn trong session của B</b> (identity map)
     * thay vì ghi giá trị mới vào nó - B vẫn nhìn thấy {@code used_count} CŨ. Đo
     * được bằng đơn hàng thật: hai tài khoản đặt gần như đồng thời với mã
     * {@code usageLimit=1}, cả hai đều tạo đơn thành công (cả hai đều được giảm
     * giá!) mà {@code used_count} cuối cùng chỉ lên 1, không phải 2 - bị B ghi đè
     * lại đúng giá trị A vừa ghi, giống hệt lost update ban đầu dù đã có khoá.
     * Bắt buộc phải {@code entityManager.refresh(locked)} ngay sau khi giữ được
     * khoá để nạp lại state mới nhất từ CSDL, ghi đè state cũ trong session.</p>
     */
    @Transactional
    public CouponUsage recordUsage(Promotion promotion, User user, Booking booking,
                                   BigDecimal discount) {
        Promotion locked = promotionRepository.findByIdForUpdate(promotion.getId())
                .orElseThrow(() -> new BusinessRuleException("error.coupon.notFound", promotion.getCode()));
        // Bắt buộc - xem Javadoc phía trên. Không có dòng này thì khoá vẫn đúng ở
        // tầng CSDL nhưng vô nghĩa ở tầng Java vì đọc nhầm giá trị cũ trong cache.
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

    /**
     * Tính lại số tiền giảm của một đơn đang dùng mã, sau khi số khách của một
     * dòng bị sửa (xem {@code BookingService.updateDetailQuantity}).
     *
     * <p>Trước bản vá này, sửa số khách chỉ gọi lại {@code Booking.recalculateTotal()}
     * - phương thức đó chỉ <b>kẹp trần</b> {@code discountAmount} không cho vượt
     * quá tiền hàng mới, chứ không tính lại số tiền giảm theo đúng luật của
     * {@link Promotion} (phần trăm nhân tiền hàng rồi kẹp trần {@code maxDiscount},
     * hay kiểm lại {@code minOrderAmount}). Hậu quả: giảm số khách để tiền hàng
     * tụt xuống dưới mức tối thiểu của mã vẫn giữ nguyên số tiền đã giảm lúc đặt -
     * khách được giảm giá cho một đơn lẽ ra không đủ điều kiện áp mã nữa.</p>
     *
     * <p>Không làm gì nếu đơn không dùng mã nào ({@code booking.getPromotion() == null}).
     * Cập nhật luôn dòng {@code coupon_usages} tương ứng cho khớp - đây là bản ghi
     * "đã dùng bao nhiêu" hiển thị ở màn quản trị mã khuyến mãi, để nó không đứng
     * yên với con số cũ trong khi đơn đã đổi.</p>
     */
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

    /**
     * Trả lại một lượt dùng mã khi đơn dùng mã đó bị huỷ (mục 12.3 - khách tự
     * huỷ; mục 12.4 - hệ thống tự huỷ đơn quá hạn thanh toán). Không làm gì nếu
     * đơn không dùng mã nào.
     *
     * <p>Xoá hẳn dòng {@code coupon_usages} (không chỉ đổi cờ): mã giới hạn
     * "1 lượt/người" mà không trả lại dòng này thì một đơn bị huỷ vẫn tính là đã
     * dùng, khách mất oan một lượt cho một đơn không còn hiệu lực.</p>
     *
     * <p>Cùng khoá bi quan + {@code refresh()} như {@link #recordUsage} - xem
     * Javadoc ở đó về gotcha lost-update khi entity đã nằm sẵn trong persistence
     * context từ trước lúc xin khoá.</p>
     */
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

    // =====================================================================
    //  Phần dành cho khu vực quản trị (UC019)
    // =====================================================================

    public static final int ADMIN_PAGE_SIZE = 15;

    /** Số lượt dùng trên một trang ở màn "xem coupon_usages của mã". */
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

    /** Lượt dùng của một mã, mới nhất trước - phục vụ "ai đã dùng, đơn nào, giảm bao nhiêu". */
    @Transactional(readOnly = true)
    public Page<CouponUsage> usagesOf(Long promotionId, int page) {
        return couponUsageRepository.findByPromotionIdOrderByUsedAtDesc(promotionId,
                PageRequest.of(Math.max(page, 0), USAGE_PAGE_SIZE));
    }

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

    /**
     * Xoá một mã khuyến mãi.
     *
     * <p><b>Chặn xoá khi mã đã có lượt dùng</b> - đúng khuôn mẫu bốn quy tắc chặn
     * xoá sẵn có (danh mục / tour / đợt khởi hành / tài khoản): kiểm bằng Java
     * qua {@code countByPromotionId}, ném {@link BusinessRuleException} kèm
     * message key và gợi ý "Vô hiệu hoá mã" thay vì xoá. Xoá một mã đã phát sinh
     * {@code coupon_usages} sẽ phá vỡ lịch sử đơn hàng đã dùng mã đó.</p>
     */
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

    /** Bật / tắt mã - phương án thay thế khi không xoá được. */
    @Transactional
    public boolean toggleActive(Long id) {
        Promotion promotion = getById(id);
        promotion.setActive(!promotion.isActive());
        return promotion.isActive();
    }
}
