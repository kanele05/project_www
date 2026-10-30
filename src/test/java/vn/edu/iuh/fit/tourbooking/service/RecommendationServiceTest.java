package vn.edu.iuh.fit.tourbooking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.edu.iuh.fit.tourbooking.ai.TourVectorService;
import vn.edu.iuh.fit.tourbooking.entity.EventType;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.entity.TourCategory;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserEventRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    private UserEventRepository events;
    private TourRepository tours;
    private TourVectorService vector;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        events = mock(UserEventRepository.class);
        tours = mock(TourRepository.class);
        vector = mock(TourVectorService.class);
        service = new RecommendationService(events, tours, vector);
    }

    static Tour tour(long id, long categoryId, long views) {
        TourCategory c = new TourCategory();
        c.setId(categoryId);
        Tour t = new Tour();
        t.setId(id);
        t.setCode("T" + id);
        t.setCategory(c);
        t.setViewCount(views);
        t.setActive(true);
        return t;
    }

    @Test
    void interest_uses_weights_view_1_click_2_cart_3() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, EventType.VIEW});
        rows.add(new Object[]{1L, EventType.CLICK});
        rows.add(new Object[]{2L, EventType.ADD_TO_CART});
        rows.add(new Object[]{3L, EventType.TIME_ON_PAGE});
        var interest = RecommendationService.score(rows);

        assertThat(interest).containsEntry(1L, 3).containsEntry(2L, 3).doesNotContainKey(3L);
        assertThat(RecommendationService.topSeeds(Map.of(1L, 1, 2L, 5, 3L, 3, 4L, 2))).containsExactly(2L, 3L, 4L);
    }

    @Test
    void rerank_mixes_similarity_and_popularity() {
        var ranked = RecommendationService.rank(
                Map.of(10L, 1.0, 11L, 0.9, 12L, 0.1),
                Map.of(10L, 0L, 11L, 1000L, 12L, 1000L), 2);
        assertThat(ranked).containsExactly(11L, 10L);
    }

    @Test
    void no_history_falls_back_to_popular_tours() {
        when(events.recentSignalsBySession(any(), any(), any())).thenReturn(List.of());
        when(tours.findByActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tour(7, 1, 900), tour(8, 1, 500))));

        var result = service.recommend(null, "sess", 2);

        assertThat(result.source()).isEqualTo(RecommendationService.Source.POPULAR);
        assertThat(result.tours()).extracting(Tour::getId).containsExactly(7L, 8L);
    }

    @Test
    void history_with_ai_off_uses_same_category_and_skips_seen_tours() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, EventType.VIEW});
        when(events.recentSignalsBySession(any(), any(), any())).thenReturn(rows);
        when(vector.isAvailable()).thenReturn(false);
        when(tours.findWithCategoryById(1L)).thenReturn(Optional.of(tour(1, 5, 10)));
        when(tours.findByCategoryIdAndIdNotAndActiveTrue(anyLong(), anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tour(2, 5, 50), tour(3, 5, 40))));

        var result = service.recommend(null, "sess", 3);

        assertThat(result.source()).isEqualTo(RecommendationService.Source.CATEGORY);
        assertThat(result.tours()).extracting(Tour::getId).containsExactly(2L, 3L);
    }

    @Test
    void history_with_ai_on_uses_vector_neighbours_excluding_seen() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, EventType.CLICK});
        when(events.recentSignalsBySession(any(), any(), any())).thenReturn(rows);
        when(vector.isAvailable()).thenReturn(true);
        when(vector.similar(1L, 8)).thenReturn(List.of(
                new TourVectorService.ScoredTour(1L, 1.0),
                new TourVectorService.ScoredTour(4L, 0.8),
                new TourVectorService.ScoredTour(5L, 0.6)));
        when(tours.findByIdInAndActiveTrue(any(Collection.class)))
                .thenReturn(List.of(tour(4, 1, 10), tour(5, 1, 10)));

        var result = service.recommend(null, "sess", 2);

        assertThat(result.source()).isEqualTo(RecommendationService.Source.VECTOR);
        assertThat(result.tours()).extracting(Tour::getId).containsExactly(4L, 5L);
    }

    @Test
    void vector_failure_degrades_gracefully() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, EventType.VIEW});
        when(events.recentSignalsBySession(any(), any(), any())).thenReturn(rows);
        when(vector.isAvailable()).thenReturn(true);
        when(vector.similar(anyLong(), org.mockito.ArgumentMatchers.anyInt())).thenThrow(new RuntimeException("429"));
        when(tours.findWithCategoryById(1L)).thenReturn(Optional.of(tour(1, 5, 10)));
        when(tours.findByCategoryIdAndIdNotAndActiveTrue(anyLong(), anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tour(2, 5, 50))));

        assertThat(service.recommend(null, "sess", 3).source()).isEqualTo(RecommendationService.Source.CATEGORY);
    }
}
