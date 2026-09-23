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

/**
 * Số liệu tổng hợp cho bảng điều khiển quản trị.
 *
 * <p>Mọi phép đếm và cộng đều được đẩy xuống CSDL bằng các truy vấn
 * {@code countBy…} / {@code SUM(…)} chứ không nạp danh sách rồi đếm trong Java.
 * Với vài chục dòng dữ liệu mẫu thì cách nào cũng chạy, nhưng khi bảng đơn hàng
 * lên vài chục nghìn dòng thì nạp hết ra bộ nhớ để đếm là hỏng hẳn.</p>
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    /** Số tháng hiển thị trên biểu đồ doanh thu. */
    private static final int REVENUE_MONTHS = 12;

    /**
     * Mục 12.9: "Doanh thu" và "Tour bán chạy" chỉ tính đơn ĐÃ XÁC NHẬN + HOÀN
     * TẤT - loại CHỜ (tiền chưa chắc thu được) và ĐÃ HUỶ (tiền không còn thu).
     */
    private static final Set<BookingStatus> REVENUE_STATUSES =
            EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final TourRepository tourRepository;
    private final TourCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /** Các con số hiện ở hàng thẻ trên cùng của bảng điều khiển. */
    public record Overview(long tourCount,
                           long categoryCount,
                           long customerCount,
                           long bookingCount,
                           BigDecimal revenue,
                           Map<BookingStatus, Long> bookingsByStatus) {
    }

    /**
     * Một cột trong biểu đồ doanh thu theo tháng.
     *
     * @param percentOfMax chiều dài thanh biểu đồ, tính sẵn theo phần trăm so với
     *                     tháng cao nhất. Tính ở đây chứ không tính trong khuôn mẫu:
     *                     Thymeleaf không có hàm lấy giá trị lớn nhất của một danh
     *                     sách ({@code #aggregates} chỉ có {@code sum} và {@code avg}),
     *                     và phép tính thuộc về tầng nghiệp vụ chứ không phải giao diện.
     */
    public record MonthlyRevenue(int year, int month, BigDecimal amount, int percentOfMax) {
        public String label() {
            return String.format("%02d/%d", month, year);
        }
    }

    /** Một dòng trong bảng xếp hạng tour bán chạy. */
    public record TopTour(String tourName, long guests) {
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        Map<BookingStatus, Long> byStatus = new EnumMap<>(BookingStatus.class);
        for (BookingStatus status : BookingStatus.values()) {
            byStatus.put(status, bookingRepository.countByStatus(status));
        }

        return new Overview(
                tourRepository.count(),
                categoryRepository.count(),
                // Chỉ đếm khách hàng, không tính tài khoản quản trị vào "số khách".
                userRepository.countByRoleAndEnabledTrue(Role.CUSTOMER),
                bookingRepository.count(),
                // Mục 12.9: chỉ tính đơn ĐÃ XÁC NHẬN + HOÀN TẤT vào doanh thu.
                bookingRepository.sumTotalAmountForStatuses(REVENUE_STATUSES),
                byStatus);
    }

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
            // Tháng cao nhất chiếm trọn 100%; nếu tất cả đều bằng 0 thì để 0 cho
            // khỏi chia cho 0.
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

    /** Năm đơn mới nhất, hiển thị ở cuối bảng điều khiển. */
    @Transactional(readOnly = true)
    public List<Booking> recentBookings() {
        return bookingRepository.findTop5ByOrderByBookingDateDesc();
    }
}
