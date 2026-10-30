package vn.edu.iuh.fit.tourbooking.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.iuh.fit.tourbooking.entity.EventType;

@Converter(autoApply = true)
// Chuyển đổi EventType <-> chuỗi lưu trong CSDL (thay cho @Enumerated, tránh sinh CHECK constraint).
public class EventTypeConverter implements AttributeConverter<EventType, String> {

    @Override
    public String convertToDatabaseColumn(EventType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public EventType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EventType.valueOf(dbData.trim());
    }
}
