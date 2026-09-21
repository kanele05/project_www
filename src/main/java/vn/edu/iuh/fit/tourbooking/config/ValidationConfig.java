package vn.edu.iuh.fit.tourbooking.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Nối Bean Validation với {@code MessageSource} của ứng dụng.
 *
 * <p>Mặc định, các thông điệp dạng <code>{validation.email.required}</code> được
 * Hibernate Validator tra trong file {@code ValidationMessages.properties} riêng
 * của nó. Khai báo bean này khiến chúng được tra trong
 * {@code messages.properties} - cùng một chỗ với mọi câu chữ khác của website.</p>
 *
 * <p>Không có bước này thì khi làm đa ngôn ngữ ở giai đoạn sau, giao diện sẽ
 * chuyển sang tiếng Anh nhưng riêng các dòng báo lỗi dưới ô nhập vẫn là tiếng
 * Việt - lỗi trông rất nghiệp dư và chỉ lộ ra đúng lúc trình bày.</p>
 */
@Configuration
public class ValidationConfig {

    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setValidationMessageSource(messageSource);
        return factory;
    }
}
