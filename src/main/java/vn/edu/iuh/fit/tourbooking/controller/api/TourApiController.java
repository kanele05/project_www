package vn.edu.iuh.fit.tourbooking.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.tourbooking.ai.SemanticSearchService;
import vn.edu.iuh.fit.tourbooking.dto.form.TourSearchForm;
import vn.edu.iuh.fit.tourbooking.dto.view.DepartureDto;
import vn.edu.iuh.fit.tourbooking.dto.view.PageResponse;
import vn.edu.iuh.fit.tourbooking.dto.view.TourDetailDto;
import vn.edu.iuh.fit.tourbooking.dto.view.TourSummaryDto;
import vn.edu.iuh.fit.tourbooking.mapper.DepartureMapper;
import vn.edu.iuh.fit.tourbooking.mapper.TourMapper;
import vn.edu.iuh.fit.tourbooking.service.TourService;

import java.util.List;

@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
// REST: chi tiết tour và danh sách lịch khởi hành - phục vụ ô chọn ngày bằng AJAX ở trang chi tiết tour.
public class TourApiController {

    private final TourService tourService;
    private final TourMapper tourMapper;
    private final DepartureMapper departureMapper;
    private final SemanticSearchService semanticSearchService;

    @GetMapping
    public PageResponse<TourSummaryDto> list(@ModelAttribute TourSearchForm form,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "9") int size) {
        return PageResponse.of(tourService.search(form, page, size), tourMapper::toSummary);
    }

    @GetMapping("/suggest")
    public List<TourSummaryDto> suggest(@RequestParam(name = "q", required = false) String keyword,
                                        @RequestParam(defaultValue = "6") int limit) {
        return tourService.suggest(keyword, limit).stream()
                .map(tourMapper::toSuggestion)
                .toList();
    }

    public record SemanticResponse(String mode, boolean aiAvailable, List<TourSummaryDto> tours) {
    }

    @GetMapping("/semantic")
    public SemanticResponse semantic(@RequestParam(name = "q", required = false) String query,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam(required = false) java.math.BigDecimal maxPrice,
                                     @RequestParam(defaultValue = "9") int size) {
        if (query == null || query.isBlank()) {
            return new SemanticResponse("KEYWORD", semanticSearchService.isAiAvailable(), List.of());
        }
        TourSearchForm form = new TourSearchForm();
        form.setQ(query);
        form.setAi(true);
        form.setCategoryId(categoryId);
        form.setMaxPrice(maxPrice);
        SemanticSearchService.Outcome outcome = semanticSearchService.search(form, 0);
        int limit = Math.clamp(size, 1, TourService.MAX_API_PAGE_SIZE);
        return new SemanticResponse(outcome.mode().name(), semanticSearchService.isAiAvailable(),
                outcome.page().getContent().stream().limit(limit).map(tourMapper::toSummary).toList());
    }

    @GetMapping("/{id}")
    public TourDetailDto detail(@PathVariable Long id) {
        return tourMapper.toDetail(tourService.findDetail(id));
    }

    @GetMapping("/{id}/departures")
    public List<DepartureDto> departures(@PathVariable Long id) {
        tourService.requireExists(id);
        return tourService.findBookableDepartures(id).stream()
                .map(departure -> departureMapper.toDto(departure, id))
                .toList();
    }
}
