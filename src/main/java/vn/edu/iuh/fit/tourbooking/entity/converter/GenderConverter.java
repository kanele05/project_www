package vn.edu.iuh.fit.tourbooking.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.iuh.fit.tourbooking.entity.Gender;

@Converter(autoApply = true)
// Chuyển đổi Gender <-> chuỗi lưu trong CSDL (thay cho @Enumerated, tránh sinh CHECK constraint).
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Gender.valueOf(dbData.trim());
    }
}
