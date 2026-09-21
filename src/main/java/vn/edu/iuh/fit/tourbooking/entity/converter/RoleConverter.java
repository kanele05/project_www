package vn.edu.iuh.fit.tourbooking.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.iuh.fit.tourbooking.entity.Role;

/**
 * Chuyển đổi {@link Role} &harr; chuỗi khi đọc/ghi CSDL.
 *
 * <p><b>Vì sao phải viết bộ chuyển đổi này thay vì dùng {@code @Enumerated(STRING)}:</b>
 * đề bài cấm dùng ràng buộc CHECK trong CSDL, mọi kiểm tra dữ liệu phải nằm ở
 * tầng Model. Nhưng từ phiên bản 6.2, Hibernate <i>tự động</i> sinh thêm một
 * ràng buộc CHECK liệt kê các giá trị hợp lệ cho mỗi cột enum - đã kiểm chứng
 * trên chính CSDL của đồ án:</p>
 *
 * <pre>
 * CK__users__role__693CA210  ([role]='ADMIN' OR [role]='CUSTOMER')
 * </pre>
 *
 * <p>Khai báo {@code columnDefinition = "VARCHAR(20)"} <b>không</b> ngăn được
 * điều đó (đã thử). Khi dùng bộ chuyển đổi, Hibernate chỉ còn thấy một cột
 * chuỗi bình thường nên không sinh CHECK nữa, còn việc chỉ chấp nhận đúng các
 * vai trò hợp lệ do {@link #convertToEntityAttribute(String)} đảm nhiệm.</p>
 *
 * <p>{@code autoApply = true} nên mọi thuộc tính kiểu {@code Role} đều được áp
 * dụng, không cần ghi {@code @Convert} ở từng entity.</p>
 */
@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role attribute) {
        return attribute == null ? null : attribute.name();
    }

    /**
     * Giá trị lạ trong CSDL sẽ ném lỗi ngay lúc đọc - đây chính là phần "kiểm
     * tra hợp lệ" mà ràng buộc CHECK vốn đảm nhiệm, nay chuyển về tầng Model.
     */
    @Override
    public Role convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Role.valueOf(dbData.trim());
    }
}
