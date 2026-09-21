package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Biểu mẫu gửi liên hệ công khai (UC021).
 *
 * <p>Không đòi đăng nhập - {@code AC01 Khách vãng lai} cũng gửi được, nên không
 * có trường {@code userId} nào ở đây. {@code tourId} tuỳ chọn: có giá trị khi
 * biểu mẫu được mở từ nút "Liên hệ về tour này" ở trang chi tiết tour, để trống
 * khi khách vào thẳng {@code /contact} hỏi chung.</p>
 */
@Data
public class ContactForm {

    /** Tour khách đang hỏi, nếu có - điền sẵn qua tham số {@code ?tourId=}. */
    private Long tourId;

    @NotBlank(message = "{validation.contact.fullName.required}")
    @Size(max = 100, message = "{validation.contact.fullName.size}")
    private String fullName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 150, message = "{validation.email.size}")
    private String email;

    @Size(max = 20, message = "{validation.contact.phone.size}")
    private String phone;

    @NotBlank(message = "{validation.contact.subject.required}")
    @Size(max = 200, message = "{validation.contact.subject.size}")
    private String subject;

    @NotBlank(message = "{validation.contact.content.required}")
    @Size(max = 2000, message = "{validation.contact.content.size}")
    private String content;
}
