package vn.edu.iuh.fit.tourbooking.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.tourbooking.config.AiProperties;
import vn.edu.iuh.fit.tourbooking.dto.form.TourSearchForm;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.service.TourService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
// Tìm tour bằng ngôn ngữ tự nhiên: vector search + lọc giá/danh mục; AI tắt, lỗi hoặc không có kết quả thì lùi về tìm từ khoá LIKE sẵn có.
public class SemanticSearchService {

    public enum Mode { KEYWORD, SEMANTIC, FALLBACK }

    public record Outcome(Page<Tour> page, Mode mode) {
    }

    private final TourVectorService vectorService;
    private final TourService tourService;
    private final TourRepository tourRepository;
    private final AiProperties properties;

    public boolean isAiAvailable() {
        return vectorService.isAvailable();
    }

    // Tìm cho trang danh sách: form.ai=false thì tìm từ khoá bình thường; true thì thử ngữ nghĩa trước.
    public Outcome search(TourSearchForm form, int page) {
        if (!Boolean.TRUE.equals(form.getAi()) || !form.hasKeyword()) {
            return new Outcome(tourService.search(form, page), Mode.KEYWORD);
        }
        try {
            List<Tour> ranked = semanticTours(form);
            if (!ranked.isEmpty()) {
                return new Outcome(slice(ranked, page), Mode.SEMANTIC);
            }
        } catch (RuntimeException e) {
            log.warn("Tìm ngữ nghĩa lỗi, lùi về tìm từ khoá: {}", e.getMessage());
        }
        return new Outcome(tourService.search(keywordFallbackForm(form), page), Mode.FALLBACK);
    }

    // Tìm cho chatbot: trả tối đa "limit" tour, ngữ nghĩa trước rồi mới tới từ khoá.
    public List<Tour> searchList(String query, Long maxPrice, int limit) {
        TourSearchForm form = new TourSearchForm();
        form.setQ(query);
        form.setAi(true);
        if (maxPrice != null && maxPrice > 0) {
            form.setMaxPrice(BigDecimal.valueOf(maxPrice));
        }
        try {
            List<Tour> ranked = semanticTours(form);
            if (!ranked.isEmpty()) {
                return ranked.stream().limit(limit).toList();
            }
        } catch (RuntimeException e) {
            log.warn("Công cụ tìm tour: ngữ nghĩa lỗi, lùi về từ khoá: {}", e.getMessage());
        }
        return tourService.search(keywordFallbackForm(form), 0, limit).getContent();
    }

    private List<Tour> semanticTours(TourSearchForm form) {
        if (!vectorService.isAvailable()) {
            return List.of();
        }
        PriceHintParser.Hint hint = PriceHintParser.parse(form.getQ());
        Long min = form.getMinPrice() != null ? Long.valueOf(form.getMinPrice().longValue()) : hint.minPrice();
        Long max = form.getMaxPrice() != null ? Long.valueOf(form.getMaxPrice().longValue()) : hint.maxPrice();

        List<TourVectorService.ScoredTour> hits = vectorService.search(
                form.getQ().trim(),
                new TourVectorService.SearchFilter(min, max, form.getCategoryId()),
                properties.searchTopK());
        if (hits.isEmpty()) {
            return List.of();
        }

        Map<Long, Tour> byId = tourRepository
                .findByIdInAndActiveTrue(hits.stream().map(TourVectorService.ScoredTour::tourId).toList())
                .stream().collect(Collectors.toMap(Tour::getId, Function.identity()));
        String destination = form.getDestination();
        return hits.stream()
                .map(h -> byId.get(h.tourId()))
                .filter(t -> t != null)
                .filter(t -> destination == null || destination.isBlank() || destination.equals(t.getDestination()))
                .toList();
    }

    // Bản sao form dùng cho tìm từ khoá dự phòng: bỏ cụm chỉ giá khỏi câu và chuyển cụm đó thành bộ lọc giá.
    TourSearchForm keywordFallbackForm(TourSearchForm form) {
        PriceHintParser.Hint hint = PriceHintParser.parse(form.getQ());
        TourSearchForm copy = new TourSearchForm();
        copy.setQ(hint.cleanedQuery());
        copy.setCategoryId(form.getCategoryId());
        copy.setDestination(form.getDestination());
        copy.setMinPrice(form.getMinPrice() != null ? form.getMinPrice()
                : hint.minPrice() == null ? null : BigDecimal.valueOf(hint.minPrice()));
        copy.setMaxPrice(form.getMaxPrice() != null ? form.getMaxPrice()
                : hint.maxPrice() == null ? null : BigDecimal.valueOf(hint.maxPrice()));
        copy.setSort(form.getSort());
        return copy;
    }

    private Page<Tour> slice(List<Tour> ranked, int page) {
        int size = TourService.PAGE_SIZE;
        int from = Math.min(Math.max(page, 0) * size, ranked.size());
        int to = Math.min(from + size, ranked.size());
        return new PageImpl<>(ranked.subList(from, to), PageRequest.of(Math.max(page, 0), size), ranked.size());
    }
}
