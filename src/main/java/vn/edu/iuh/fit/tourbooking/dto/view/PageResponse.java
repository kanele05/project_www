package vn.edu.iuh.fit.tourbooking.dto.view;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Khuôn phân trang của các web service.
 *
 * <p><b>Vì sao không trả thẳng {@code Page} của Spring Data:</b> JSON do nó sinh
 * ra chứa cả cấu trúc nội bộ ({@code pageable}, {@code sort.unsorted},
 * {@code numberOfElements}...) mà Spring vẫn ghi rõ là <i>không cam kết giữ ổn
 * định giữa các phiên bản</i> - nâng cấp Spring Boot là phía trình duyệt hỏng
 * theo. Lớp này chỉ công bố đúng bảy con số mà giao diện thực sự dùng.</p>
 *
 * @param content    dữ liệu của trang hiện tại, đã đổi sang DTO
 * @param page       số thứ tự trang, bắt đầu từ 0 giống Spring Data
 * @param totalPages tổng số trang, dùng để dựng thanh phân trang
 */
public record PageResponse<T>(List<T> content,
                              int page,
                              int size,
                              long totalElements,
                              int totalPages,
                              boolean first,
                              boolean last) {

    /**
     * Đổi một trang entity thành trang DTO.
     *
     * <p>Nhận sẵn hàm chuyển đổi thay vì để nơi gọi tự map rồi mới dựng: như vậy
     * không có chỗ nào quên chép lại một trong sáu con số phân trang.</p>
     */
    public static <E, D> PageResponse<D> of(Page<E> source, Function<E, D> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast());
    }
}
