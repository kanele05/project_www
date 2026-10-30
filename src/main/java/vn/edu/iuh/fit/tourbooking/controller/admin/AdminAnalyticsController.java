package vn.edu.iuh.fit.tourbooking.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.iuh.fit.tourbooking.service.BehaviorStatsService;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
// Trang thống kê hành vi người dùng: lọc theo khoảng ngày, hiện top xem/click, phễu chuyển đổi, từ khoá, thời gian xem.
public class AdminAnalyticsController {

    private final BehaviorStatsService statsService;

    @GetMapping
    public String analytics(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            Model model) {
        BehaviorStatsService.Report report = statsService.report(from, to);
        model.addAttribute("report", report);
        model.addAttribute("today", LocalDate.now());
        return "admin/analytics";
    }
}
