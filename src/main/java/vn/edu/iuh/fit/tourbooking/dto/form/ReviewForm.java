package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Biểu mẫu đánh giá tour (UC018) - gửi từ trang chi tiết tour.
 *
 * <p>Không có {@code userId}: người đánh giá luôn lấy từ tài khoản đang đăng
 * nhập ({@code principal.getId()}), giống nguyên tắc ở {@code AccountController}.
 * Khoảng giá trị 1-5 sao kiểm ở đây (DTO) chứ không đặt CHECK trong CSDL, đúng
 * quy ước "ràng buộc nằm ở tầng Java" của đề bài.</p>
 */
@Data
public class ReviewForm {

    @NotNull
    private Long tourId;

    @NotNull(message = "{validation.review.rating.required}")
    @Min(value = 1, message = "{validation.review.rating.range}")
    @Max(value = 5, message = "{validation.review.rating.range}")
    private Integer rating = 5;

    @Size(max = 200, message = "{validation.review.title.size}")
    private String title;

    @NotBlank(message = "{validation.review.content.required}")
    @Size(max = 1000, message = "{validation.review.content.size}")
    private String content;
}
