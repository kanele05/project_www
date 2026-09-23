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

/**
 * Biểu mẫu thêm / sửa mã khuyến mãi (UC019).
 *
 * <p>Dùng chung một biểu mẫu cho cả thêm mới lẫn sửa, cùng khuôn mẫu với
 * {@link CategoryForm} và {@link vn.edu.iuh.fit.tourbooking.dto.form.TourForm}.
 * {@code @DateTimeFormat(iso = DATE_TIME)} là bắt buộc: ô
 * {@code <input type="datetime-local">} gửi lên chuỗi dạng
 * {@code 2026-08-22T09:00}, không có chú thích này Spring không đổi được thành
 * {@code LocalDateTime}.</p>
 */
@Data
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

    /** Chỉ có nghĩa với {@link DiscountType#PERCENT}; để trống nghĩa là không chặn trần. */
    private BigDecimal maxDiscount;

    @NotNull(message = "{validation.promotion.minOrderAmount.required}")
    @DecimalMin(value = "0", message = "{validation.promotion.minOrderAmount.min}")
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Min(value = 1, message = "{validation.promotion.usageLimit.min}")
    private Integer usageLimit;

    /**
     * Mặc định {@code 1}: để trống ô này trên biểu mẫu từng khiến quy tắc
     * "1 mã / 1 tài khoản" âm thầm biến mất mà không có cảnh báo nào (xem
     * {@link Promotion#usageLimitPerUser} - null nghĩa là không giới hạn).
     * Quản trị viên vẫn xoá trắng được nếu thật sự muốn không giới hạn.
     */
    @Min(value = 1, message = "{validation.promotion.usageLimitPerUser.min}")
    private Integer usageLimitPerUser = 1;

    @NotNull(message = "{validation.promotion.startAt.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startAt;

    @NotNull(message = "{validation.promotion.endAt.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endAt;

    private boolean active = true;

    /** Ngày kết thúc phải sau ngày bắt đầu - kiểm ngay ở biểu mẫu cho phản hồi tức thì. */
    @AssertTrue(message = "{validation.promotion.period.invalid}")
    public boolean isPeriodValid() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }

    /**
     * {@code maxDiscount} để trống nghĩa là không chặn trần (xem trường phía
     * trên) nên không thể gắn {@code @DecimalMin} trực tiếp lên trường - annotation
     * đó không phân biệt được "chưa nhập" với "nhập số âm". Thiếu ràng buộc này,
     * nhập {@code 0} hoặc số âm khiến {@code Promotion.calculateDiscount} luôn trả
     * về một số {@code <= 0}, và {@code PromotionService.check} kết luận sai lý do
     * là "chưa đạt giá trị đơn tối thiểu" ({@code error.coupon.minOrder}) cho MỌI
     * lần áp mã - kể cả đơn thừa sức đạt {@code minOrderAmount}.
     */
    @AssertTrue(message = "{validation.promotion.maxDiscount.positive}")
    public boolean isMaxDiscountValid() {
        return maxDiscount == null || maxDiscount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * {@code discountValue} chỉ có {@code @DecimalMin("0.01")}, không có trần -
     * loại {@link DiscountType#PERCENT} với giá trị {@code 200} từng lọt qua,
     * khiến {@code Promotion.calculateDiscount} ra {@code orderAmount * 2} rồi bị
     * {@code .min(orderAmount)} kẹp lại đúng bằng giá trị đơn: khách trả 0 đồng.
     * Loại {@link DiscountType#AMOUNT} không bị chặn ở đây vì đơn vị là tiền, một
     * số tiền lớn hơn đơn hàng đã tự kẹp về giá trị đơn một cách hợp lý.
     */
    @AssertTrue(message = "{validation.promotion.discountValue.percentMax}")
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
