package vn.edu.iuh.fit.tourbooking.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.tourbooking.dto.form.TourSearchForm;
import vn.edu.iuh.fit.tourbooking.dto.view.DepartureDto;
import vn.edu.iuh.fit.tourbooking.dto.view.PageResponse;
import vn.edu.iuh.fit.tourbooking.dto.view.TourDetailDto;
import vn.edu.iuh.fit.tourbooking.dto.view.TourSummaryDto;
import vn.edu.iuh.fit.tourbooking.mapper.DepartureMapper;
import vn.edu.iuh.fit.tourbooking.mapper.TourMapper;
import vn.edu.iuh.fit.tourbooking.service.TourService;

import java.util.List;

/**
 * Web service về tour (CLO6).
 *
 * <p>Đọc lại đúng những nghiệp vụ mà giao diện web đang dùng - {@code TourService}
 * là một, chỉ khác cách trình bày kết quả. Không nhân đôi logic tìm kiếm sang đây:
 * nếu mai kia đổi quy tắc lọc, chỉ có một chỗ phải sửa.</p>
 *
 * <p><b>Mọi phương thức đều trả DTO, không trả entity.</b> Trả thẳng {@code Tour}
 * sẽ kéo theo các quan hệ LAZY lúc Jackson tuần tự hoá và công bố ra ngoài cả
 * những cột nội bộ - xem chú thích ở {@code TourSummaryDto}.</p>
 */
@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class TourApiController {

    private final TourService tourService;
    private final TourMapper tourMapper;
    private final DepartureMapper departureMapper;

    /**
     * Danh sách tour, dùng chung bộ lọc với trang web.
     *
     * <p>{@code @ModelAttribute} gom mọi tham số truy vấn ({@code q},
     * {@code categoryId}, {@code minPrice}...) vào {@link TourSearchForm} - đúng
     * lớp mà trang danh sách đang dùng, nên hai bên không thể hiểu khác nhau về
     * ý nghĩa của một bộ lọc.</p>
     */
    @GetMapping
    public PageResponse<TourSummaryDto> list(@ModelAttribute TourSearchForm form,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "9") int size) {
        return PageResponse.of(tourService.search(form, page, size), tourMapper::toSummary);
    }

    /**
     * Gợi ý cho ô tìm kiếm.
     *
     * <p>Tách khỏi {@link #list} vì hai việc khác nhau: gợi ý cần trả lời thật
     * nhanh sau mỗi phím gõ nên chỉ lấy vài dòng, không phân trang, không nạp
     * danh mục.</p>
     *
     * <p><b>Phải khai báo trước {@code /{id}}</b> - nếu không, Spring sẽ thử khớp
     * {@code /api/tours/suggest} với {@code /api/tours/{id}} và báo lỗi không đổi
     * được chữ "suggest" thành số.</p>
     */
    @GetMapping("/suggest")
    public List<TourSummaryDto> suggest(@RequestParam(name = "q", required = false) String keyword,
                                        @RequestParam(defaultValue = "6") int limit) {
        return tourService.suggest(keyword, limit).stream()
                .map(tourMapper::toSuggestion)
                .toList();
    }

    /** Chi tiết một tour. Không có thì {@code ApiExceptionHandler} đổi thành 404. */
    @GetMapping("/{id}")
    public TourDetailDto detail(@PathVariable Long id) {
        return tourMapper.toDetail(tourService.findDetail(id));
    }

    /**
     * Các đợt khởi hành còn nhận khách của một tour - đây là web service phục vụ
     * ô chọn ngày bằng AJAX ở trang chi tiết.
     *
     * <p>Kiểm tra tour có tồn tại trước khi trả danh sách: hỏi đợt khởi hành của
     * một tour không có thật phải nhận <b>404</b>, chứ trả về một mảng rỗng thì
     * phía trình duyệt tưởng tour tồn tại nhưng hết chỗ - hai chuyện hoàn toàn
     * khác nhau.</p>
     */
    @GetMapping("/{id}/departures")
    public List<DepartureDto> departures(@PathVariable Long id) {
        tourService.requireExists(id);
        return tourService.findBookableDepartures(id).stream()
                .map(departure -> departureMapper.toDto(departure, id))
                .toList();
    }
}
