package vn.edu.iuh.fit.tourbooking.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;

/**
 * Chuyển đổi {@link BookingStatus} &harr; chuỗi khi đọc/ghi CSDL.
 *
 * <p>Cùng lý do với {@link RoleConverter}: tránh việc Hibernate 6.2+ tự sinh
 * ràng buộc CHECK cho cột enum, vốn bị đề bài cấm. Xem phần chú thích chi tiết
 * ở lớp đó.</p>
 */
@Converter(autoApply = true)
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
