package vn.edu.iuh.fit.tourbooking.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.tourbooking.dto.view.RevenuePointDto;
import vn.edu.iuh.fit.tourbooking.service.StatisticsService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
// REST AJAX: dữ liệu vẽ biểu đồ doanh thu trên trang chủ khu quản trị.
public class AdminStatsApiController {

    private static final int DEFAULT_TOP_TOURS = 5;

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public StatisticsService.Overview overview() {
        return statisticsService.overview();
    }

    @GetMapping("/revenue")
    public List<RevenuePointDto> revenue() {
        return statisticsService.revenueByMonth().stream()
                .map(row -> new RevenuePointDto(row.label(), row.year(), row.month(),
                        row.amount(), row.percentOfMax()))
                .toList();
    }

    @GetMapping("/top-tours")
    public List<StatisticsService.TopTour> topTours(
            @RequestParam(defaultValue = "" + DEFAULT_TOP_TOURS) int limit) {
        return statisticsService.topSellingTours(Math.clamp(limit, 1, 20));
    }
}
