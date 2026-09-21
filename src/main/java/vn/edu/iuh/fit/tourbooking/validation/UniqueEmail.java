package vn.edu.iuh.fit.tourbooking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kiểm tra email chưa có ai dùng.
 *
 * <p>Ràng buộc UNIQUE trong CSDL vẫn là chốt chặn cuối, nhưng nếu chỉ dựa vào nó
 * thì người dùng nhận được một trang lỗi 500 khó hiểu thay vì dòng chữ đỏ ngay
 * dưới ô nhập. Kiểm tra ở tầng Model đúng với yêu cầu của đề bài và cũng là
 * cách duy nhất báo lỗi tử tế.</p>
 *
 * <p>Khi <b>sửa</b> hồ sơ, phải bỏ qua chính bản ghi đang sửa - nếu không người
 * dùng giữ nguyên email của mình cũng bị báo trùng. Đó là việc của thuộc tính
 * {@link #excludeIdField()}.</p>
 */
@Documented
@Constraint(validatedBy = UniqueEmailValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueEmail {

    String message() default "{validation.email.duplicate}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Tên trường chứa email trong biểu mẫu. */
    String emailField() default "email";

    /**
     * Tên trường chứa mã người dùng đang sửa. Để trống (hoặc giá trị null) nghĩa
     * là đang thêm mới, khi đó mọi email đã tồn tại đều bị coi là trùng.
     */
    String excludeIdField() default "";
}
