package vn.edu.iuh.fit.tourbooking.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.iuh.fit.tourbooking.entity.DiscountType;

/**
 * Chuyển đổi {@link DiscountType} &harr; chuỗi khi đọc/ghi CSDL.
 *
 * <p>Cùng lý do với {@link RoleConverter}: Hibernate 6.2+ tự sinh ràng buộc CHECK
 * cho cột nào ánh xạ bằng {@code @Enumerated}, mà đề bài cấm CHECK trong CSDL.
 * Quy ước của dự án: <b>mọi enum thêm mới đều phải có converter riêng như file
 * này</b>, và tuyệt đối không dùng {@code @Enumerated}.</p>
 */
@Converter(autoApply = true)
public class DiscountTypeConverter implements AttributeConverter<DiscountType, String> {

    @Override
    public String convertToDatabaseColumn(DiscountType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public DiscountType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DiscountType.valueOf(dbData.trim());
    }
}
