package vn.edu.iuh.fit.tourbooking.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.iuh.fit.tourbooking.service.StatisticsService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
// Trang chủ khu quản trị: số liệu tổng quan, đơn gần đây, tour bán chạy.
public class AdminDashboardController {

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
