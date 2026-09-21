package vn.edu.iuh.fit.tourbooking.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.iuh.fit.tourbooking.service.StatisticsService;

/**
 * Bảng điều khiển của khu vực quản trị.
 *
 * <p>Cả nhánh {@code /admin/**} đã được {@code SecurityConfig} đặt ở mức
 * {@code hasRole('ADMIN')}, nên không lớp nào trong gói này phải tự kiểm tra
 * quyền nữa - kiểm tra một chỗ vẫn hơn rải rác mỗi nơi một ít rồi quên.</p>
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    /** Số tour hiển thị trong bảng xếp hạng bán chạy. */
    private static final int TOP_TOUR_LIMIT = 5;

    private final StatisticsService statisticsService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("overview", statisticsService.overview());
        model.addAttribute("revenueByMonth", statisticsService.revenueByMonth());
        model.addAttribute("topTours", statisticsService.topSellingTours(TOP_TOUR_LIMIT));
        model.addAttribute("recentBookings", statisticsService.recentBookings());
        return "admin/dashboard";
    }
}
