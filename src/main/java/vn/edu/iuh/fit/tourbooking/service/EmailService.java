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

    /**
     * Gửi thư chào mừng sau khi đăng ký tài khoản thành công.
     *
     * <p>Nhận đúng hai chuỗi thay vì cả {@code User} entity: người gọi nằm trong
     * {@code UserEmailListener}, chạy ở một giao dịch khác (hoặc ngoài giao dịch)
     * so với lúc tạo tài khoản, nên đọc thuộc tính từ một entity đã tách khỏi
     * ngữ cảnh lưu trữ (detached) là không an toàn. Hai chuỗi này không có gì để
     * mà lazy-load, nên tránh được hẳn vấn đề đó.</p>
     *
     * <p><b>Không bao giờ</b> nhận hay in ra mật khẩu, kể cả mật khẩu đã băm.</p>
     */
    void sendWelcomeEmail(String fullName, String email);
}
