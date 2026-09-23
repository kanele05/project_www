package vn.edu.iuh.fit.tourbooking.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.edu.iuh.fit.tourbooking.entity.PaymentMethod;

@Converter(autoApply = true)
// Chuyển đổi PaymentMethod <-> chuỗi lưu trong CSDL (thay cho @Enumerated, tránh sinh CHECK constraint).
public class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {

    @Override
    public String convertToDatabaseColumn(PaymentMethod attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PaymentMethod convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PaymentMethod.valueOf(dbData.trim());
    }
}
