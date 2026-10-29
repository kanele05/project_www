package vn.edu.iuh.fit.tourbooking.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.edu.iuh.fit.tourbooking.config.AiProperties;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.exception.AiUnavailableException;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TourVectorServiceTest {

    @TempDir
    Path dir;

    private HashingEmbeddingModel embeddings;
    private TourVectorStore store;
    private TourRepository tours;
    private AiProperties props;
    private TourVectorService service;

    @BeforeEach
    void setUp() {
        embeddings = new HashingEmbeddingModel();
        store = new TourVectorStore(embeddings);
        tours = mock(TourRepository.class);
        List<Tour> all = AiTestSupport.sampleTours();
        when(tours.findByActiveTrue()).thenReturn(all);
        for (Tour t : all) {
            when(tours.findWithCategoryById(t.getId())).thenReturn(Optional.of(t));
        }
        props = AiTestSupport.props(dir, "model-a");
        service = new TourVectorService(AiTestSupport.provider(TourVectorStore.class, store), tours, props,
                new ObjectMapper());
    }

    @Test
    void reindex_indexes_active_tours_and_persists_file_and_manifest() {
        var result = service.reindexAll();

        assertThat(result.indexed()).isEqualTo(5);
        assertThat(store.ids()).containsExactlyInAnyOrder("tour-1", "tour-2", "tour-3", "tour-4", "tour-5");
        assertThat(embeddings.calls).as("SimpleVectorStore nhúng từng tài liệu một").isEqualTo(5);
        assertThat(dir.resolve("vector-store.json")).exists();
        assertThat(dir.resolve("vector-store.json.meta.json")).exists();
        assertThat(service.status().indexedCount()).isEqualTo(5);
    }

    @Test
    void reindex_is_an_upsert_not_a_duplicate_and_removes_stale_tours() {
        service.reindexAll();
        List<Tour> fewer = AiTestSupport.sampleTours().subList(0, 3);
        when(tours.findByActiveTrue()).thenReturn(fewer);

        var result = service.reindexAll();

        assertThat(result.indexed()).isEqualTo(3);
        assertThat(result.removed()).isEqualTo(2);
        assertThat(store.ids()).containsExactlyInAnyOrder("tour-1", "tour-2", "tour-3");
    }

    @Test
    void semantic_search_ranks_by_meaning_and_applies_price_and_category_filters() {
        service.reindexAll();

        var beach = service.search("biển đảo lặn ngắm san hô", TourVectorService.SearchFilter.none(), 5);
        assertThat(beach).isNotEmpty();
        assertThat(beach.get(0).tourId()).isEqualTo(1L);

        var cheap = service.search("biển đảo", new TourVectorService.SearchFilter(null, 5_000_000L, null), 5);
        assertThat(cheap).extracting(TourVectorService.ScoredTour::tourId).contains(2L).doesNotContain(1L);

        var mountains = service.search("núi", new TourVectorService.SearchFilter(null, null, 2L), 5);
        assertThat(mountains).extracting(TourVectorService.ScoredTour::tourId)
                .isNotEmpty().allMatch(id -> id >= 3);

        var band = service.search("núi rừng", new TourVectorService.SearchFilter(3_000_000L, 4_300_000L, null), 5);
        assertThat(band).extracting(TourVectorService.ScoredTour::tourId).containsExactlyInAnyOrder(3L, 5L);
    }

    @Test
    void similar_excludes_the_tour_itself_and_is_cached() {
        service.reindexAll();
        int before = embeddings.calls;

        var first = service.similar(3L, 2);
        assertThat(first).extracting(TourVectorService.ScoredTour::tourId).doesNotContain(3L);
        assertThat(first.get(0).tourId()).isIn(4L, 5L);
        int afterFirst = embeddings.calls;
        assertThat(afterFirst).isEqualTo(before + 1);

        service.similar(3L, 2);
        assertThat(embeddings.calls).as("lần hai lấy từ bộ nhớ đệm").isEqualTo(afterFirst);
    }

    @Test
    void restart_loads_the_saved_file_without_calling_the_embedding_api_and_filters_still_work() {
        service.reindexAll();

        var freshEmbeddings = new HashingEmbeddingModel();
        var freshStore = new TourVectorStore(freshEmbeddings);
        var fresh = new TourVectorService(AiTestSupport.provider(TourVectorStore.class, freshStore), tours, props,
                new ObjectMapper());
        fresh.loadOrIndex();

        assertThat(freshStore.ids()).hasSize(5);
        assertThat(freshEmbeddings.calls).as("không nhúng lại khi khởi động").isZero();
        var cheap = fresh.search("biển đảo", new TourVectorService.SearchFilter(null, 5_000_000L, null), 5);
        assertThat(cheap).extracting(TourVectorService.ScoredTour::tourId).contains(2L).doesNotContain(1L);
        var notSelf = fresh.similar(1L, 3);
        assertThat(notSelf).extracting(TourVectorService.ScoredTour::tourId).doesNotContain(1L);
    }

    @Test
    void changing_the_embedding_model_forces_a_rebuild() {
        service.reindexAll();

        var otherProps = AiTestSupport.props(dir, "model-b");
        var freshEmbeddings = new HashingEmbeddingModel();
        var freshStore = new TourVectorStore(freshEmbeddings);
        var fresh = new TourVectorService(AiTestSupport.provider(TourVectorStore.class, freshStore), tours,
                otherProps, new ObjectMapper());
        fresh.loadOrIndex();

        assertThat(freshEmbeddings.calls).isPositive();
    }

    @Test
    void upsert_adds_and_deactivated_or_deleted_tours_are_removed() {
        service.reindexAll();
        Tour hidden = AiTestSupport.sampleTours().get(0);
        hidden.setActive(false);
        when(tours.findWithCategoryById(1L)).thenReturn(Optional.of(hidden));
        service.upsert(1L);
        assertThat(store.ids()).doesNotContain("tour-1");

        hidden.setActive(true);
        service.upsert(1L);
        assertThat(store.ids()).contains("tour-1");

        when(tours.findWithCategoryById(2L)).thenReturn(Optional.empty());
        service.upsert(2L);
        assertThat(store.ids()).doesNotContain("tour-2");
    }

    @Test
    void without_a_store_the_service_reports_unavailable_and_noops() {
        var off = new TourVectorService(AiTestSupport.provider(TourVectorStore.class, null), tours, props,
                new ObjectMapper());

        assertThat(off.isAvailable()).isFalse();
        assertThat(off.status().available()).isFalse();
        off.upsert(1L);
        off.remove(1L);
        assertThatThrownBy(() -> off.search("x", TourVectorService.SearchFilter.none(), 3))
                .isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(off::reindexAll).isInstanceOf(AiUnavailableException.class);
        assertThat(Files.exists(dir.resolve("vector-store.json"))).isFalse();
    }

    @Test
    void provider_failure_during_reindex_becomes_a_friendly_exception_and_keeps_the_old_index() {
        service.reindexAll();
        var failing = new HashingEmbeddingModel() {
            @Override
            public org.springframework.ai.embedding.EmbeddingResponse call(
                    org.springframework.ai.embedding.EmbeddingRequest request) {
                throw new IllegalStateException("429 Too Many Requests");
            }
        };
        var failingStore = new TourVectorStore(failing);
        var svc = new TourVectorService(AiTestSupport.provider(TourVectorStore.class, failingStore), tours,
                props, new ObjectMapper());

        assertThatThrownBy(svc::reindexAll).isInstanceOf(AiUnavailableException.class);
        assertThat(svc.status().lastError()).contains("429");
        assertThat(store.ids()).hasSize(5);
    }

    @Test
    void document_carries_filterable_metadata() {
        var doc = TourDocumentFactory.toDocument(AiTestSupport.sampleTours().get(0));
        assertThat(doc.getId()).isEqualTo("tour-1");
        assertThat(doc.getMetadata()).containsEntry("tourId", 1L).containsEntry("price", 5_990_000L)
                .containsEntry("categoryId", 1L).containsEntry("destination", "Phú Quốc");
        assertThat(doc.getText()).contains("Phú Quốc").contains("biển");
    }
}
