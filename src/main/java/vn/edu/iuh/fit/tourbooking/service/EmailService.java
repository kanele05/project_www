package vn.edu.iuh.fit.tourbooking.service;

import vn.edu.iuh.fit.tourbooking.entity.Booking;

/**
 * Gửi thư cho khách hàng.
 *
 * <p>Tách thành giao diện để có hai bản cài đặt: một bản chỉ in ra màn hình
 * (dùng khi phát triển và khi trình bày, không cần tài khoản SMTP thật, không sợ
 * lỡ tay gửi thư tới địa chỉ có thật trong dữ liệu mẫu) và một bản gửi thật qua
 * SMTP. Chọn bản nào là do thuộc tính {@code app.mail.mode} trong
 * {@code application.yml} quyết định - đổi một dòng cấu hình, không phải sửa mã.</p>
 */
public interface EmailService {

    /**
     * Gửi thư xác nhận đặt tour.
     *
     * <p>Nơi gọi phải truyền vào một {@code Booking} đã nạp sẵn danh sách chi tiết
     * và đợt khởi hành, vì phương thức này chạy ngoài giao dịch.</p>
     */
    void sendBookingConfirmation(Booking booking);
}
