package vn.edu.iuh.fit.tourbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
@EnableScheduling
// Điểm khởi động Spring Boot: bật JPA Auditing, quét @ConfigurationProperties, bật bộ hẹn giờ (@Scheduled).
public class TourBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TourBookingApplication.class, args);
    }
}
