package vn.edu.iuh.fit.tourbooking.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

/**
 * Dựng nội dung thư xác nhận đặt tour.
 *
 * <p>Nội dung thư được render bằng chính Thymeleaf đang dùng cho website
 * ({@code templates/email/booking-confirmation.html}) chứ không ghép chuỗi HTML
 * trong mã Java. Nhờ vậy sửa mẫu thư không phải biên dịch lại, và khi làm đa
 * ngôn ngữ thì thư cũng tự chuyển ngữ theo cùng bộ {@code messages.properties}.</p>
 *
 * <p>Tách riêng khỏi hai bản cài đặt {@code EmailService} vì cả hai đều cần dựng
 * nội dung giống hệt nhau - bản in ra màn hình cũng phải dựng thật để lỗi trong
 * mẫu thư lộ ra ngay lúc phát triển, chứ không đợi tới lúc bật SMTP mới biết.</p>
 */
@Component
@RequiredArgsConstructor
public class BookingEmailRenderer {

    private final SpringTemplateEngine templateEngine;
    private final MessageHelper messages;

    /**
     * Tiêu đề thư, cũng lấy từ {@code messages.properties} như mọi câu chữ khác -
     * ghép chuỗi trong Java thì bản tiếng Anh sẽ có đúng dòng tiêu đề trơ ra
     * không dịch được.
     */
    public String subject(Booking booking) {
        return messages.get("email.booking.subject", booking.getCode());
    }

    public String htmlBody(Booking booking) {
        Context context = new Context(LocaleContextHolder.getLocale());
        context.setVariable("booking", booking);
        return templateEngine.process("email/booking-confirmation", context);
    }

    /**
     * Bản tóm tắt dạng chữ thuần, dùng cho nhật ký khi chạy ở chế độ console -
     * đổ cả trang HTML ra log thì không ai đọc nổi.
     */
    public String plainSummary(Booking booking) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mã đơn      : ").append(booking.getCode()).append('\n');
        sb.append("Khách hàng  : ").append(booking.getCustomerName())
                .append(" <").append(booking.getCustomerEmail()).append(">\n");
        sb.append("Điện thoại  : ").append(booking.getCustomerPhone()).append('\n');
        sb.append("Trạng thái  : ").append(booking.getStatus().getDisplayName()).append('\n');
        booking.getDetails().forEach(d ->
                sb.append("  - ").append(d.getTourNameSnapshot())
                        .append(" | khởi hành ").append(d.getDeparture().getDepartureDate())
                        .append(" | ").append(d.getNumAdults()).append(" người lớn, ")
                        .append(d.getNumChildren()).append(" trẻ em")
                        .append(" | ").append(d.getSubtotal()).append(" đ\n"));
        sb.append("Tổng cộng   : ").append(booking.getTotalAmount()).append(" đ");
        return sb.toString();
    }
}
