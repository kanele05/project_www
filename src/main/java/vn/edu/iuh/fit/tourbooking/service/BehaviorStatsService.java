package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.entity.EventType;
import vn.edu.iuh.fit.tourbooking.repository.BookingDetailRepository;
import vn.edu.iuh.fit.tourbooking.repository.BookingRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserEventRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
// Thống kê hành vi người dùng theo khoảng ngày: top xem/click, phễu xem -> giỏ -> đơn, đơn nhiều nhất, từ khoá, thời gian xem.
public class BehaviorStatsService {

    public static final int DEFAULT_DAYS = 30;

    public static final int MAX_DAYS = 365;

    private static final int TOP_LIMIT = 10;

    private final UserEventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;

    public record Range(LocalDate from, LocalDate to) {
        public LocalDateTime start() {
            return from.atStartOfDay();
        }

        public LocalDateTime endExclusive() {
            return to.plusDays(1).atStartOfDay();
        }
    }

    public record TourCount(Long tourId, String tourName, long count) {
    }

    public record KeywordCount(String keyword, long count) {
    }

    public record TourDuration(Long tourId, String tourName, double avgSeconds, long samples) {
    }

    public record FunnelStep(String key, long value, int percentOfTop) {
    }

    public record TourFunnelRow(Long tourId, String tourName, long views, long clicks,
                                long carts, long bookings) {
        public double clickRate() {
            return ratio(clicks, views);
        }

        public double cartRate() {
            return ratio(carts, views);
        }

        public double bookingRate() {
            return ratio(bookings, carts);
        }
    }

    public record Report(Range range,
                         long totalEvents,
                         long sessions,
                         List<TourCount> topViewed,
                         List<TourCount> topClicked,
                         List<FunnelStep> funnel,
                         List<TourFunnelRow> tourFunnel,
                         List<TourCount> mostBooked,
                         List<KeywordCount> keywords,
                         Double avgSecondsOnPage,
                         List<TourDuration> longestReads) {
    }

    // Chuẩn hoá khoảng ngày: thiếu thì lấy 30 ngày gần nhất, đảo ngược thì đổi chỗ, tối đa 365 ngày.
    public static Range normalize(LocalDate from, LocalDate to, LocalDate today) {
        LocalDate end = to == null ? today : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_DAYS - 1L) : from;
        if (start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }
        if (start.isBefore(end.minusDays(MAX_DAYS - 1L))) {
            start = end.minusDays(MAX_DAYS - 1L);
        }
        return new Range(start, end);
    }

    // Dựng toàn bộ báo cáo hành vi cho trang quản trị trong một giao dịch chỉ đọc.
    @Transactional(readOnly = true)
    public Report report(LocalDate from, LocalDate to) {
        Range range = normalize(from, to, LocalDate.now());
        LocalDateTime start = range.start();
        LocalDateTime end = range.endExclusive();
        PageRequest top = PageRequest.of(0, TOP_LIMIT);

        long viewSessions = eventRepository.countDistinctSessionsByType(EventType.VIEW, start, end);
        long cartSessions = eventRepository.countDistinctSessionsByType(EventType.ADD_TO_CART, start, end);
        long bookings = bookingRepository.countPlacedBetween(start, end);

        List<TourFunnelRow> tourFunnel = tourFunnel(start, end);
        Double avgMs = eventRepository.averageDuration(EventType.TIME_ON_PAGE, start, end);
        Double avgSeconds = avgMs == null ? null : avgMs / 1000.0;

        return new Report(
                range,
                eventRepository.countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(start, end),
                eventRepository.countDistinctSessions(start, end),
                tourCounts(eventRepository.topToursByType(EventType.VIEW, start, end, top)),
                tourCounts(eventRepository.topToursByType(EventType.CLICK, start, end, top)),
                funnel(viewSessions, cartSessions, bookings),
                tourFunnel,
                mostBooked(start, end),
                keywords(start, end, top),
                avgSeconds,
                durations(start, end, top));
    }

    // Phễu ba bước (số phiên có xem tour, số phiên có thêm giỏ, số đơn đặt) kèm tỉ lệ so với bước đầu để vẽ thanh.
    public static List<FunnelStep> funnel(long viewSessions, long cartSessions, long bookings) {
        long top = Math.max(viewSessions, Math.max(cartSessions, bookings));
        return List.of(
                new FunnelStep("view", viewSessions, percent(viewSessions, top)),
                new FunnelStep("cart", cartSessions, percent(cartSessions, top)),
                new FunnelStep("booking", bookings, percent(bookings, top)));
    }

    public static double ratio(long part, long whole) {
        return whole <= 0 ? 0.0 : (double) part / whole;
    }

    private static int percent(long value, long top) {
        return top <= 0 ? 0 : (int) Math.round(value * 100.0 / top);
    }

    private List<TourFunnelRow> tourFunnel(LocalDateTime start, LocalDateTime end) {
        Map<Long, Long> bookingsByTour = new HashMap<>();
        for (Object[] row : bookingDetailRepository.bookingCountByTour(start, end)) {
            bookingsByTour.put(((Number) row[0]).longValue(), ((Number) row[2]).longValue());
        }

        List<TourFunnelRow> rows = new ArrayList<>();
        for (Object[] row : eventRepository.funnelByTour(
                EventType.VIEW, EventType.CLICK, EventType.ADD_TO_CART, start, end)) {
            long tourId = ((Number) row[0]).longValue();
            rows.add(new TourFunnelRow(tourId, (String) row[1],
                    ((Number) row[2]).longValue(), ((Number) row[3]).longValue(),
                    ((Number) row[4]).longValue(), bookingsByTour.getOrDefault(tourId, 0L)));
        }
        rows.sort(Comparator.comparingLong(TourFunnelRow::views).reversed()
                .thenComparing(TourFunnelRow::tourName));
        return rows.stream().limit(TOP_LIMIT).toList();
    }

    private List<TourCount> mostBooked(LocalDateTime start, LocalDateTime end) {
        return bookingDetailRepository.bookingCountByTour(start, end).stream()
                .limit(TOP_LIMIT)
                .map(row -> new TourCount(((Number) row[0]).longValue(), (String) row[1],
                        ((Number) row[2]).longValue()))
                .toList();
    }

    private List<TourCount> tourCounts(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new TourCount(((Number) row[0]).longValue(), (String) row[1],
                        ((Number) row[2]).longValue()))
                .toList();
    }

    private List<KeywordCount> keywords(LocalDateTime start, LocalDateTime end, PageRequest top) {
        return eventRepository.topKeywords(EventType.SEARCH, start, end, top).stream()
                .map(row -> new KeywordCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    private List<TourDuration> durations(LocalDateTime start, LocalDateTime end, PageRequest top) {
        return eventRepository.averageDurationByTour(EventType.TIME_ON_PAGE, start, end, top).stream()
                .map(row -> new TourDuration(((Number) row[0]).longValue(), (String) row[1],
                        ((Number) row[2]).doubleValue() / 1000.0, ((Number) row[3]).longValue()))
                .toList();
    }
}
