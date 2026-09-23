package vn.edu.iuh.fit.tourbooking.dto.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
// Biểu mẫu liên hệ công khai.
public class ContactForm {

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
