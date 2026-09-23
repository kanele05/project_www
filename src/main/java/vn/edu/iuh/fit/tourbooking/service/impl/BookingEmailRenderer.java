package vn.edu.iuh.fit.tourbooking.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

@Component
@RequiredArgsConstructor
// Dựng tiêu đề, nội dung HTML (qua Thymeleaf) và bản tóm tắt văn bản của thư xác nhận đặt tour.
public class BookingEmailRenderer {

    private final SpringTemplateEngine templateEngine;
    private final MessageHelper messages;

    public String subject(Booking booking) {
        return messages.get("email.booking.subject", booking.getCode());
    }

    public String htmlBody(Booking booking) {
        Context context = new Context(LocaleContextHolder.getLocale());
        context.setVariable("booking", booking);
        return templateEngine.process("email/booking-confirmation", context);
    }

    // Ghép các dòng chi tiết đơn thành một đoạn văn bản thuần để ghi log/gửi thư dạng chữ.
    public String plainSummary(Booking booking) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mã đơn      : ").append(booking.getCode()).append('\n');
        sb.append("Khách hàng  : ").append(booking.getCustomerName())
                .append(" <").append(booking.getCustomerEmail()).append(">\n");
        sb.append("Điện thoại  : ").append(booking.getCustomerPhone()).append('\n');

        sb.append("Trạng thái  : ").append(messages.get(booking.getStatus().getMessageKey())).append('\n');
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
