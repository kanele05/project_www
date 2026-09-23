package vn.edu.iuh.fit.tourbooking.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.iuh.fit.tourbooking.entity.PassengerType;

@Converter(autoApply = true)
// Chuyển đổi PassengerType <-> chuỗi lưu trong CSDL (thay cho @Enumerated, tránh sinh CHECK constraint).
public class PassengerTypeConverter implements AttributeConverter<PassengerType, String> {

    @Override
    public String convertToDatabaseColumn(PassengerType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PassengerType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PassengerType.valueOf(dbData.trim());
    }
}
