package vn.edu.iuh.fit.tourbooking.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;

@Converter(autoApply = true)
// Chuyển đổi BookingStatus <-> chuỗi lưu trong CSDL (thay cho @Enumerated, tránh sinh CHECK constraint).
public class BookingStatusConverter implements AttributeConverter<BookingStatus, String> {

    @Override
    public String convertToDatabaseColumn(BookingStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public BookingStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BookingStatus.valueOf(dbData.trim());
    }
}
