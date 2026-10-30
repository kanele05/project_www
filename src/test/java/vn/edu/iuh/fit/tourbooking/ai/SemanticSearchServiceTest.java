package vn.edu.iuh.fit.tourbooking.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import vn.edu.iuh.fit.tourbooking.dto.form.TourSearchForm;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.exception.AiUnavailableException;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.service.TourService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SemanticSearchServiceTest {

    private TourVectorService vector;
    private TourService tourService;
    private TourRepository tours;
    private SemanticSearchService service;
    private final List<Tour> sample = AiTestSupport.sampleTours();

    @BeforeEach
    void setUp() {
        vector = mock(TourVectorService.class);
        tourService = mock(TourService.class);
        tours = mock(TourRepository.class);
        service = new SemanticSearchService(vector, tourService, tours, AiTestSupport.props(Path.of("unused"), "m"));
        Page<Tour> keywordPage = new PageImpl<>(List.of(sample.get(3)));
        when(tourService.search(any(TourSearchForm.class), anyInt())).thenReturn(keywordPage);
        when(tourService.search(any(TourSearchForm.class), anyInt(), anyInt())).thenReturn(keywordPage);
    }

    private TourSearchForm form(String q, Boolean ai) {
        TourSearchForm f = new TourSearchForm();
        f.setQ(q);
        f.setAi(ai);
        return f;
    }

    @Test
    void ai_toggle_off_uses_plain_keyword_search_and_never_touches_the_vector_store() {
        var outcome = service.search(form("đà lạt", false), 0);
        assertThat(outcome.mode()).isEqualTo(SemanticSearchService.Mode.KEYWORD);
        verify(vector, never()).search(any(), any(), anyInt());
    }

    @Test
    void ai_disabled_falls_back_to_keyword_search_with_price_phrase_converted_to_a_filter() {
        when(vector.isAvailable()).thenReturn(false);

        var outcome = service.search(form("biển yên tĩnh dưới 5 triệu", true), 0);

        assertThat(outcome.mode()).isEqualTo(SemanticSearchService.Mode.FALLBACK);
        var captor = org.mockito.ArgumentCaptor.forClass(TourSearchForm.class);
        verify(tourService).search(captor.capture(), eq(0));
        assertThat(captor.getValue().getQ()).isEqualTo("biển yên tĩnh");
        assertThat(captor.getValue().getMaxPrice()).isEqualByComparingTo(BigDecimal.valueOf(5_000_000));
    }

    @Test
    void semantic_hits_are_returned_in_rank_order_with_price_filter_from_the_query() {
        when(vector.isAvailable()).thenReturn(true);
        when(vector.search(eq("biển dưới 5 triệu"), any(), anyInt())).thenReturn(List.of(
                new TourVectorService.ScoredTour(2L, 0.9), new TourVectorService.ScoredTour(1L, 0.8)));
        when(tours.findByIdInAndActiveTrue(any(Collection.class))).thenReturn(List.of(sample.get(0), sample.get(1)));

        var outcome = service.search(form("biển dưới 5 triệu", true), 0);

        assertThat(outcome.mode()).isEqualTo(SemanticSearchService.Mode.SEMANTIC);
        assertThat(outcome.page().getContent()).extracting(Tour::getId).containsExactly(2L, 1L);
        var filter = org.mockito.ArgumentCaptor.forClass(TourVectorService.SearchFilter.class);
        verify(vector).search(any(), filter.capture(), anyInt());
        assertThat(filter.getValue().maxPrice()).isEqualTo(5_000_000L);
    }

    @Test
    void explicit_form_price_overrides_the_phrase_in_the_query() {
        when(vector.isAvailable()).thenReturn(true);
        when(vector.search(any(), any(), anyInt())).thenReturn(List.of());
        var f = form("biển dưới 5 triệu", true);
        f.setMaxPrice(BigDecimal.valueOf(3_000_000));

        service.search(f, 0);

        var filter = org.mockito.ArgumentCaptor.forClass(TourVectorService.SearchFilter.class);
        verify(vector).search(any(), filter.capture(), anyInt());
        assertThat(filter.getValue().maxPrice()).isEqualTo(3_000_000L);
    }

    @Test
    void provider_error_or_empty_semantic_result_falls_back_instead_of_failing() {
        when(vector.isAvailable()).thenReturn(true);
        when(vector.search(any(), any(), anyInt())).thenThrow(new AiUnavailableException("error.ai.unavailable"));
        assertThat(service.search(form("núi", true), 0).mode()).isEqualTo(SemanticSearchService.Mode.FALLBACK);

        org.mockito.Mockito.doReturn(List.of()).when(vector).search(any(), any(), anyInt());
        assertThat(service.search(form("núi", true), 0).mode()).isEqualTo(SemanticSearchService.Mode.FALLBACK);
    }

    @Test
    void chatbot_search_list_falls_back_to_keywords_when_ai_is_off() {
        when(vector.isAvailable()).thenReturn(false);
        assertThat(service.searchList("đà lạt", null, 5)).hasSize(1);
    }
}
