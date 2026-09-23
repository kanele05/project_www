package vn.edu.iuh.fit.tourbooking.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.iuh.fit.tourbooking.entity.ContactStatus;

@Converter(autoApply = true)
// Chuyển đổi ContactStatus <-> chuỗi lưu trong CSDL (thay cho @Enumerated, tránh sinh CHECK constraint).
public class ContactStatusConverter implements AttributeConverter<ContactStatus, String> {

    @Override
    public String convertToDatabaseColumn(ContactStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ContactStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ContactStatus.valueOf(dbData.trim());
    }
}
