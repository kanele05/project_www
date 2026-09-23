package vn.edu.iuh.fit.tourbooking.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.iuh.fit.tourbooking.entity.DiscountType;

@Converter(autoApply = true)
// Chuyển đổi DiscountType <-> chuỗi lưu trong CSDL (thay cho @Enumerated, tránh sinh CHECK constraint).
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
