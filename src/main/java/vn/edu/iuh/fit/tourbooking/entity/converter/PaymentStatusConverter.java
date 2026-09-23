package vn.edu.iuh.fit.tourbooking.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.iuh.fit.tourbooking.entity.PaymentStatus;

@Converter(autoApply = true)
// Chuyển đổi PaymentStatus <-> chuỗi lưu trong CSDL (thay cho @Enumerated, tránh sinh CHECK constraint).
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(PaymentStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PaymentStatus.valueOf(dbData.trim());
    }
}
