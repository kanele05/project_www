package vn.edu.iuh.fit.tourbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.tourbooking.ai.TourVectorService;
import vn.edu.iuh.fit.tourbooking.entity.EventType;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserEventRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
// "Có thể bạn thích": từ các tour vừa xem/bấm/thêm giỏ của tài khoản hoặc phiên, tìm tour giống bằng vector rồi xếp lại có tính độ phổ biến; thiếu AI hoặc lịch sử thì lùi về cùng danh mục, rồi tour phổ biến.
public class RecommendationService {

    public enum Source { VECTOR, CATEGORY, POPULAR }

    public record Recommendation(List<Tour> tours, Source source) {
    }

    static final double SIMILARITY_WEIGHT = 0.7;
    static final double POPULARITY_WEIGHT = 0.3;

    private static final int SIGNAL_DAYS = 30;
    private static final int SIGNAL_ROWS = 100;
    private static final int SEED_TOURS = 3;
    private static final int NEIGHBOURS_PER_SEED = 8;

    private final UserEventRepository eventRepository;
    private final TourRepository tourRepository;
    private final TourVectorService vectorService;

    public Recommendation recommend(Long userId, String sessionId, int limit) {
        Map<Long, Integer> interest = interest(userId, sessionId);
        if (interest.isEmpty()) {
            return new Recommendation(popular(Set.of(), limit), Source.POPULAR);
        }
        Set<Long> seen = interest.keySet();
        List<Long> seeds = topSeeds(interest);

        if (vectorService.isAvailable()) {
            try {
                List<Tour> byVector = byVector(interest, seeds, limit);
                if (!byVector.isEmpty()) {
                    return new Recommendation(byVector, Source.VECTOR);
                }
            } catch (RuntimeException e) {
                log.warn("Gợi ý bằng vector lỗi, lùi về cùng danh mục: {}", e.getMessage());
            }
        }

        List<Tour> sameCategory = sameCategory(seeds.get(0), seen, limit);
        if (!sameCategory.isEmpty()) {
            return new Recommendation(sameCategory, Source.CATEGORY);
        }
        return new Recommendation(popular(seen, limit), Source.POPULAR);
    }

    // "Tour tương tự" cho trang chi tiết: vector giống nhất; AI tắt hoặc lỗi thì lấy tour cùng danh mục xem nhiều nhất.
    public Recommendation similarTo(Long tourId, int limit) {
        if (vectorService.isAvailable()) {
            try {
                List<Long> ids = vectorService.similar(tourId, limit).stream()
                        .map(TourVectorService.ScoredTour::tourId).toList();
                if (!ids.isEmpty()) {
                    Map<Long, Tour> byId = tourRepository.findByIdInAndActiveTrue(ids).stream()
                            .collect(Collectors.toMap(Tour::getId, Function.identity()));
                    List<Tour> ordered = ids.stream().map(byId::get).filter(t -> t != null).toList();
                    if (!ordered.isEmpty()) {
                        return new Recommendation(ordered, Source.VECTOR);
                    }
                }
            } catch (RuntimeException e) {
                log.warn("Tour tương tự bằng vector lỗi, lùi về cùng danh mục: {}", e.getMessage());
            }
        }
        return new Recommendation(sameCategory(tourId, Set.of(), limit), Source.CATEGORY);
    }

    // Điểm quan tâm theo tour = tổng trọng số sự kiện (xem 1, bấm 2, thêm giỏ 3); giữ thứ tự mới nhất trước.
    Map<Long, Integer> interest(Long userId, String sessionId) {
        LocalDateTime since = LocalDateTime.now().minusDays(SIGNAL_DAYS);
        List<Object[]> rows = new ArrayList<>();
        if (userId != null) {
            rows.addAll(eventRepository.recentSignalsByUser(userId, since, PageRequest.of(0, SIGNAL_ROWS)));
        }
        if (sessionId != null) {
            rows.addAll(eventRepository.recentSignalsBySession(
                    TrackingService.hashSession(sessionId), since, PageRequest.of(0, SIGNAL_ROWS)));
        }
        return score(rows);
    }

    static Map<Long, Integer> score(List<Object[]> rows) {
        Map<Long, Integer> interest = new LinkedHashMap<>();
        for (Object[] row : rows) {
            int weight = ((EventType) row[1]).getWeight();
            if (weight > 0) {
                interest.merge(((Number) row[0]).longValue(), weight, Integer::sum);
            }
        }
        return interest;
    }

    static List<Long> topSeeds(Map<Long, Integer> interest) {
        return interest.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(SEED_TOURS)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<Tour> byVector(Map<Long, Integer> interest, List<Long> seeds, int limit) {
        Map<Long, Double> similarity = new HashMap<>();
        for (Long seed : seeds) {
            double weight = interest.get(seed);
            for (TourVectorService.ScoredTour hit : vectorService.similar(seed, NEIGHBOURS_PER_SEED)) {
                if (!interest.containsKey(hit.tourId())) {
                    similarity.merge(hit.tourId(), weight * hit.score(), Double::sum);
                }
            }
        }
        if (similarity.isEmpty()) {
            return List.of();
        }
        Map<Long, Tour> candidates = tourRepository.findByIdInAndActiveTrue(similarity.keySet()).stream()
                .collect(Collectors.toMap(Tour::getId, Function.identity()));
        Map<Long, Long> views = new HashMap<>();
        candidates.values().forEach(t -> views.put(t.getId(), t.getViewCount()));
        return rank(similarity, views, limit).stream().map(candidates::get)
                .filter(t -> t != null).toList();
    }

    // Xếp hạng lại: 0.7 x độ giống (chuẩn hoá theo giá trị lớn nhất) + 0.3 x độ phổ biến (lượt xem chuẩn hoá).
    static List<Long> rank(Map<Long, Double> similarity, Map<Long, Long> views, int limit) {
        double maxSim = similarity.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double maxViews = views.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        Map<Long, Double> finalScore = new HashMap<>();
        similarity.forEach((id, sim) -> {
            double simPart = maxSim <= 0 ? 0 : sim / maxSim;
            double popPart = maxViews <= 0 ? 0 : views.getOrDefault(id, 0L) / maxViews;
            finalScore.put(id, SIMILARITY_WEIGHT * simPart + POPULARITY_WEIGHT * popPart);
        });
        return finalScore.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder())))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<Tour> sameCategory(Long seedTourId, Set<Long> seen, int limit) {
        Tour seed = tourRepository.findWithCategoryById(seedTourId).orElse(null);
        if (seed == null) {
            return List.of();
        }
        return tourRepository.findByCategoryIdAndIdNotAndActiveTrue(seed.getCategory().getId(), seedTourId,
                        PageRequest.of(0, limit + seen.size(), Sort.by(Sort.Direction.DESC, "viewCount")))
                .getContent().stream()
                .filter(t -> !seen.contains(t.getId()))
                .limit(limit)
                .toList();
    }

    private List<Tour> popular(Set<Long> exclude, int limit) {
        Set<Long> excluded = new LinkedHashSet<>(exclude);
        return tourRepository.findByActiveTrue(
                        PageRequest.of(0, limit + excluded.size(), Sort.by(Sort.Direction.DESC, "viewCount")))
                .getContent().stream()
                .filter(t -> !excluded.contains(t.getId()))
                .limit(limit)
                .toList();
    }
}
