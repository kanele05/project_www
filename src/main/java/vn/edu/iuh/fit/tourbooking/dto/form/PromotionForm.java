package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import vn.edu.iuh.fit.tourbooking.entity.DiscountType;
import vn.edu.iuh.fit.tourbooking.entity.Promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
// Biểu mẫu thêm/sửa mã khuyến mãi ở khu quản trị, kèm tự kiểm khoảng thời gian và mức giảm.
public class PromotionForm {

    private Long id;

    @NotBlank(message = "{validation.promotion.code.required}")
    @Size(max = 30, message = "{validation.promotion.code.size}")
    private String code;

    @NotBlank(message = "{validation.promotion.name.required}")
    @Size(max = 150, message = "{validation.promotion.name.size}")
    private String name;

    @Size(max = 500, message = "{validation.promotion.description.size}")
    private String description;

    @NotNull(message = "{validation.promotion.discountType.required}")
    private DiscountType discountType = DiscountType.PERCENT;

    @NotNull(message = "{validation.promotion.discountValue.required}")
    @DecimalMin(value = "0.01", message = "{validation.promotion.discountValue.min}")
    private BigDecimal discountValue;

    private BigDecimal maxDiscount;

    @NotNull(message = "{validation.promotion.minOrderAmount.required}")
    @DecimalMin(value = "0", message = "{validation.promotion.minOrderAmount.min}")
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Min(value = 1, message = "{validation.promotion.usageLimit.min}")
    private Integer usageLimit;

    @Min(value = 1, message = "{validation.promotion.usageLimitPerUser.min}")
    private Integer usageLimitPerUser = 1;

    @NotNull(message = "{validation.promotion.startAt.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startAt;

    @NotNull(message = "{validation.promotion.endAt.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endAt;

    private boolean active = true;

    @AssertTrue(message = "{validation.promotion.period.invalid}")
    // Ngày kết thúc phải sau ngày bắt đầu.
    public boolean isPeriodValid() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }

    @AssertTrue(message = "{validation.promotion.maxDiscount.positive}")
    // Mức giảm tối đa (nếu có nhập) phải lớn hơn 0.
    public boolean isMaxDiscountValid() {
        return maxDiscount == null || maxDiscount.compareTo(BigDecimal.ZERO) > 0;
    }

    @AssertTrue(message = "{validation.promotion.discountValue.percentMax}")
    // Giảm theo phần trăm thì không được vượt quá 100%.
    public boolean isPercentValueValid() {
        return discountType != DiscountType.PERCENT
                || discountValue == null
                || discountValue.compareTo(BigDecimal.valueOf(100)) <= 0;
    }

    public static PromotionForm from(Promotion p) {
        PromotionForm form = new PromotionForm();
        form.setId(p.getId());
        form.setCode(p.getCode());
        form.setName(p.getName());
        form.setDescription(p.getDescription());
        form.setDiscountType(p.getDiscountType());
        form.setDiscountValue(p.getDiscountValue());
        form.setMaxDiscount(p.getMaxDiscount());
        form.setMinOrderAmount(p.getMinOrderAmount());
        form.setUsageLimit(p.getUsageLimit());
        form.setUsageLimitPerUser(p.getUsageLimitPerUser());
        form.setStartAt(p.getStartAt());
        form.setEndAt(p.getEndAt());
        form.setActive(p.isActive());
        return form;
    }
}
