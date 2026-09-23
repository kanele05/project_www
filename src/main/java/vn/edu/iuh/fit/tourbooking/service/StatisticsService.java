package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.entity.Booking;
import vn.edu.iuh.fit.tourbooking.entity.BookingStatus;
import vn.edu.iuh.fit.tourbooking.entity.Role;
import vn.edu.iuh.fit.tourbooking.repository.BookingDetailRepository;
import vn.edu.iuh.fit.tourbooking.repository.BookingRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourCategoryRepository;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
// Số liệu tổng hợp cho bảng điều khiển quản trị; mọi phép đếm/cộng đẩy xuống CSDL, không nạp cả danh sách.
public class StatisticsService {

    private static final int REVENUE_MONTHS = 12;

    private static final Set<BookingStatus> REVENUE_STATUSES =
            EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final TourRepository tourRepository;
    private final TourCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public record Overview(long tourCount,
                           long categoryCount,
                           long customerCount,
                           long bookingCount,
                           BigDecimal revenue,
                           Map<BookingStatus, Long> bookingsByStatus) {
    }

    public record MonthlyRevenue(int year, int month, BigDecimal amount, int percentOfMax) {
        public String label() {
            return String.format("%02d/%d", month, year);
        }
    }

    public record TopTour(String tourName, long guests) {
    }

    // Số liệu tổng quan cho trang chủ quản trị: số tour/danh mục/khách, tổng đơn, doanh thu, đơn theo trạng thái.
    @Transactional(readOnly = true)
    public Overview overview() {
        Map<BookingStatus, Long> byStatus = new EnumMap<>(BookingStatus.class);
        for (BookingStatus status : BookingStatus.values()) {
            byStatus.put(status, bookingRepository.countByStatus(status));
        }

        return new Overview(
                tourRepository.count(),
                categoryRepository.count(),

                userRepository.countByRoleAndEnabledTrue(Role.CUSTOMER),
                bookingRepository.count(),

                bookingRepository.sumTotalAmountForStatuses(REVENUE_STATUSES),
                byStatus);
    }

    // Doanh thu 12 tháng gần nhất, kèm tỉ lệ phần trăm so với tháng cao nhất để vẽ biểu đồ.
    @Transactional(readOnly = true)
    public List<MonthlyRevenue> revenueByMonth() {
        LocalDateTime from = LocalDateTime.now().minusMonths(REVENUE_MONTHS);
        List<Object[]> rows = bookingRepository.revenueByMonth(from, REVENUE_STATUSES);
        if (rows.isEmpty()) {
            return List.of();
        }

        BigDecimal max = rows.stream()
                .map(row -> (BigDecimal) row[2])
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);

        List<MonthlyRevenue> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            BigDecimal amount = (BigDecimal) row[2];

            int percent = max.signum() == 0 ? 0
                    : amount.multiply(BigDecimal.valueOf(100))
                            .divide(max, 0, RoundingMode.HALF_UP)
                            .intValue();

            result.add(new MonthlyRevenue(
                    ((Number) row[0]).intValue(),
                    ((Number) row[1]).intValue(),
                    amount,
                    percent));
        }
        return result;
    }

    // Danh sách tour bán chạy nhất theo tổng số khách.
    @Transactional(readOnly = true)
    public List<TopTour> topSellingTours(int limit) {
        List<Object[]> rows = bookingDetailRepository.findTopSellingTours(
                REVENUE_STATUSES, PageRequest.of(0, limit));

        List<TopTour> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new TopTour((String) row[0], ((Number) row[1]).longValue()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Booking> recentBookings() {
        return bookingRepository.findTop5ByOrderByBookingDateDesc();
    }
}
