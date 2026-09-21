package vn.edu.iuh.fit.tourbooking.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.tourbooking.dto.view.RevenuePointDto;
import vn.edu.iuh.fit.tourbooking.service.StatisticsService;

import java.util.List;

/**
 * Web service số liệu cho bảng điều khiển quản trị.
 *
 * <p>Nhờ có nó, biểu đồ doanh thu vẽ được bằng dữ liệu tải riêng thay vì nhúng
 * cứng vào HTML lúc dựng trang: bấm nút "Tải lại" là số liệu mới, không phải
 * F5 cả trang.</p>
 *
 * <p>Quyền truy cập do {@code SecurityConfig} chặn ở mức đường dẫn
 * ({@code /api/admin/**} phải có vai trò ADMIN) - không dựa vào việc giấu nút
 * trên giao diện.</p>
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsApiController {

    private static final int DEFAULT_TOP_TOURS = 5;

    private final StatisticsService statisticsService;

    /** Các con số ở hàng thẻ trên cùng. */
    @GetMapping("/overview")
    public StatisticsService.Overview overview() {
        return statisticsService.overview();
    }

    /**
     * Doanh thu 12 tháng gần nhất - nguồn dữ liệu của biểu đồ.
     *
     * <p>Đổi sang {@link RevenuePointDto} để nhãn trục hoành ({@code 08/2026}) đi
     * kèm sẵn trong JSON. Bản ghi của tầng nghiệp vụ có phương thức
     * {@code label()} nhưng Jackson chỉ tuần tự hoá các thành phần của record nên
     * nhãn ấy sẽ không xuất hiện - xem chú thích trong {@code RevenuePointDto}.</p>
     */
    @GetMapping("/revenue")
    public List<RevenuePointDto> revenue() {
        return statisticsService.revenueByMonth().stream()
                .map(row -> new RevenuePointDto(row.label(), row.year(), row.month(),
                        row.amount(), row.percentOfMax()))
                .toList();
    }

    /** Bảng xếp hạng tour bán chạy. */
    @GetMapping("/top-tours")
    public List<StatisticsService.TopTour> topTours(
            @RequestParam(defaultValue = "" + DEFAULT_TOP_TOURS) int limit) {
        return statisticsService.topSellingTours(Math.clamp(limit, 1, 20));
    }
}
