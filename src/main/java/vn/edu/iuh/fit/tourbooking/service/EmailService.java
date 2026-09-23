package vn.edu.iuh.fit.tourbooking.service;

import vn.edu.iuh.fit.tourbooking.entity.Booking;

// Gửi thư xác nhận đặt tour và thư chào mừng đăng ký; có hai cách cài đặt (console/SMTP).
public interface EmailService {

    void sendBookingConfirmation(Booking booking);

    void sendWelcomeEmail(String fullName, String email);
}
