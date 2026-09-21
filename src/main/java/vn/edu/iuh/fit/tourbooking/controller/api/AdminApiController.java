package vn.edu.iuh.fit.tourbooking.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.tourbooking.security.CustomUserDetails;
import vn.edu.iuh.fit.tourbooking.service.CategoryService;
import vn.edu.iuh.fit.tourbooking.service.PromotionService;
import vn.edu.iuh.fit.tourbooking.service.TourService;
import vn.edu.iuh.fit.tourbooking.service.UserService;

/**
 * Web service xoá dữ liệu ở khu vực quản trị.
 *
 * <p><b>Đây là chỗ chứng minh rõ nhất rằng bốn quy tắc chặn xoá của đề bài được
 * kiểm tra trong chương trình chứ không phải trong CSDL.</b> Xoá một danh mục
 * còn tour sẽ nhận về <b>409 Conflict</b> kèm câu giải thích tiếng Việt và gợi ý
 * phương án thay thế - thứ mà một ràng buộc khoá ngoại không bao giờ trả về được
 * (nó chỉ ném ra một lỗi SQL rồi thành 500). Ảnh chụp phản hồi 409 này nên đưa
 * vào báo cáo.</p>
 *
 * <p>Xoá thành công trả về <b>204 No Content</b>: không còn gì để trả về, và
 * phía trình duyệt chỉ cần biết là xong.</p>
 *
 * <p>Các quy tắc chặn nằm nguyên trong tầng service, ở đây không kiểm tra lại
 * dòng nào - nhờ vậy xoá bằng biểu mẫu thường hay bằng AJAX đều đi qua đúng một
 * bộ quy tắc.</p>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminApiController {

    private final TourService tourService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final PromotionService promotionService;

    /** Bị chặn khi tour đã có lượt đặt &rarr; 409 {@code error.tour.delete.inBooking}. */
    @DeleteMapping("/tours/{id}")
    public ResponseEntity<Void> deleteTour(@PathVariable Long id) {
        tourService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Bị chặn khi danh mục còn tour &rarr; 409 {@code error.category.delete.hasTours}. */
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bị chặn khi tài khoản đã có đơn, là chính mình, hoặc là quản trị viên cuối
     * cùng &rarr; 409.
     *
     * <p>Mã tài khoản đang đăng nhập lấy từ principal, <b>không</b> nhận từ tham
     * số: quy tắc "không tự xoá chính mình" mà tin vào con số trình duyệt gửi lên
     * thì gửi số khác là lách được.</p>
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        userService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /** Bị chặn khi mã khuyến mãi đã có lượt dùng &rarr; 409 {@code error.promotion.delete.hasUsages}. */
    @DeleteMapping("/promotions/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
