package vn.edu.iuh.fit.tourbooking.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.tourbooking.config.AiProperties;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.exception.AiUnavailableException;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
// Chỉ mục vector của tour: nạp, tải lại theo yêu cầu, upsert từng tour, tìm theo ngữ nghĩa và tìm tour tương tự; AI tắt thì mọi truy vấn trả rỗng.
public class TourVectorService {

    public record ScoredTour(long tourId, double score) {
    }

    public record SearchFilter(Long minPrice, Long maxPrice, Long categoryId) {
        public static SearchFilter none() {
            return new SearchFilter(null, null, null);
        }
    }

    public record IndexResult(int indexed, int removed) {
    }

    public record IndexStatus(boolean enabled, boolean available, int indexedCount,
                              LocalDateTime indexedAt, String lastError) {
    }

    record Manifest(String embeddingKey, String indexedAt, int count) {
    }

    private final ObjectProvider<TourVectorStore> storeProvider;
    private final TourRepository tourRepository;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    private final Map<Long, List<ScoredTour>> similarCache = new ConcurrentHashMap<>();
    private volatile LocalDateTime indexedAt;
    private volatile String lastError;

    public TourVectorService(ObjectProvider<TourVectorStore> storeProvider, TourRepository tourRepository,
                             AiProperties properties, ObjectMapper objectMapper) {
        this.storeProvider = storeProvider;
        this.tourRepository = tourRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    // Có kho vector để dùng hay không (false khi AI tắt).
    public boolean isAvailable() {
        return storeProvider.getIfAvailable() != null;
    }

    public IndexStatus status() {
        TourVectorStore store = storeProvider.getIfAvailable();
        return new IndexStatus(properties.active(), store != null,
                store == null ? 0 : store.ids().size(), indexedAt, lastError);
    }

    // Lúc khởi động: nạp tệp đã lưu nếu khớp mô hình nhúng, ngược lại nạp lại toàn bộ tour từ CSDL.
    public void loadOrIndex() {
        TourVectorStore store = requireStore();
        File file = storeFile();
        Manifest manifest = readManifest();
        if (file.isFile() && manifest != null
                && embeddingKey().equals(manifest.embeddingKey())) {
            store.load(file);
            indexedAt = parseTime(manifest.indexedAt());
            if (!store.ids().isEmpty()) {
                log.info("Đã nạp {} tour từ {}", store.ids().size(), file);
                return;
            }
        }
        reindexAll();
    }

    // Nạp lại toàn bộ tour đang bán (upsert theo id xác định), gỡ các tour đã ngừng bán/bị xoá, rồi lưu ra tệp.
    public synchronized IndexResult reindexAll() {
        TourVectorStore store = requireStore();
        List<Tour> tours = tourRepository.findByActiveTrue();
        List<Document> documents = tours.stream().map(TourDocumentFactory::toDocument).toList();
        try {
            if (!documents.isEmpty()) {
                store.add(documents);
            }
            Set<String> keep = new HashSet<>();
            documents.forEach(d -> keep.add(d.getId()));
            List<String> stale = store.ids().stream().filter(id -> !keep.contains(id)).toList();
            if (!stale.isEmpty()) {
                store.delete(stale);
            }
            similarCache.clear();
            persist(store);
            lastError = null;
            log.info("Đã lập chỉ mục {} tour, gỡ {} tour cũ", documents.size(), stale.size());
            return new IndexResult(documents.size(), stale.size());
        } catch (RuntimeException e) {
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            throw new AiUnavailableException("error.ai.unavailable", e);
        }
    }

    // Cập nhật một tour (thêm/sửa); tour không còn bán hoặc đã xoá thì gỡ khỏi chỉ mục.
    public synchronized void upsert(Long tourId) {
        TourVectorStore store = storeProvider.getIfAvailable();
        if (store == null) {
            return;
        }
        Tour tour = tourRepository.findWithCategoryById(tourId).orElse(null);
        if (tour == null || !tour.isActive()) {
            remove(tourId);
            return;
        }
        store.add(List.of(TourDocumentFactory.toDocument(tour)));
        similarCache.clear();
        persist(store);
    }

    public synchronized void remove(Long tourId) {
        TourVectorStore store = storeProvider.getIfAvailable();
        if (store == null) {
            return;
        }
        String id = TourDocumentFactory.documentId(tourId);
        if (store.ids().contains(id)) {
            store.delete(List.of(id));
            similarCache.clear();
            persist(store);
        }
    }

    // Tìm tour theo ngữ nghĩa: topK, ngưỡng giống nhau và bộ lọc metadata (giá, danh mục) dựng bằng FilterExpressionBuilder.
    public List<ScoredTour> search(String query, SearchFilter filter, int topK) {
        TourVectorStore store = requireStore();
        SearchRequest.Builder request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(properties.similarityThreshold());
        FilterExpressionBuilder.Op op = buildFilter(filter, null);
        if (op != null) {
            request.filterExpression(op.build());
        }
        return toScored(store.similaritySearch(request.build()));
    }

    // Tour tương tự: lấy mô tả của chính tour làm truy vấn, loại chính nó; kết quả được nhớ đến lần lập chỉ mục sau.
    public List<ScoredTour> similar(Long tourId, int limit) {
        TourVectorStore store = requireStore();
        List<ScoredTour> cached = similarCache.get(tourId);
        if (cached == null) {
            Tour tour = tourRepository.findWithCategoryById(tourId).orElse(null);
            if (tour == null) {
                return List.of();
            }
            SearchRequest request = SearchRequest.builder()
                    .query(TourDocumentFactory.text(tour))
                    .topK(limit + 1)
                    .similarityThreshold(0.0)
                    .filterExpression(buildFilter(SearchFilter.none(), tourId).build())
                    .build();
            cached = toScored(store.similaritySearch(request));
            similarCache.put(tourId, cached);
        }
        return cached.stream().limit(limit).toList();
    }

    // Khoá nhận diện cách nhúng (nhà cung cấp + mô hình + số chiều): đổi một trong ba thì vector cũ không dùng lại được.
    String embeddingKey() {
        return properties.baseUrl() + "|" + properties.embeddingModel() + "|"
                + (properties.embeddingDimensions() == null ? "default" : properties.embeddingDimensions());
    }

    static FilterExpressionBuilder.Op buildFilter(SearchFilter filter, Long excludeTourId) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        List<FilterExpressionBuilder.Op> ops = new ArrayList<>();
        if (filter.minPrice() != null) {
            ops.add(b.gte("price", filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            ops.add(b.lte("price", filter.maxPrice()));
        }
        if (filter.categoryId() != null) {
            ops.add(b.eq("categoryId", filter.categoryId()));
        }
        if (excludeTourId != null) {
            ops.add(b.ne("tourId", excludeTourId));
        }
        return ops.stream().reduce(b::and).orElse(null);
    }

    private List<ScoredTour> toScored(List<Document> documents) {
        return documents.stream()
                .filter(d -> d.getMetadata().get("tourId") instanceof Number)
                .map(d -> new ScoredTour(((Number) d.getMetadata().get("tourId")).longValue(),
                        d.getScore() == null ? 0.0 : d.getScore()))
                .toList();
    }

    private TourVectorStore requireStore() {
        TourVectorStore store = storeProvider.getIfAvailable();
        if (store == null) {
            throw new AiUnavailableException("error.ai.disabled");
        }
        return store;
    }

    private File storeFile() {
        return new File(properties.vectorStorePath());
    }

    private File manifestFile() {
        return new File(properties.vectorStorePath() + ".meta.json");
    }

    private void persist(TourVectorStore store) {
        try {
            File file = storeFile();
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            store.save(file);
            indexedAt = LocalDateTime.now();
            objectMapper.writeValue(manifestFile(), new Manifest(
                    embeddingKey(), indexedAt.toString(), store.ids().size()));
        } catch (IOException e) {
            log.warn("Không lưu được chỉ mục vector ra tệp: {}", e.getMessage());
        }
    }

    private Manifest readManifest() {
        File file = manifestFile();
        if (!file.isFile()) {
            return null;
        }
        try {
            return objectMapper.readValue(file, Manifest.class);
        } catch (IOException e) {
            return null;
        }
    }

    private LocalDateTime parseTime(String text) {
        try {
            return text == null ? null : LocalDateTime.parse(text);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
