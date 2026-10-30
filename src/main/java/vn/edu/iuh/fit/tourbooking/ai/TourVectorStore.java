package vn.edu.iuh.fit.tourbooking.ai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.util.Set;

// SimpleVectorStore có thêm hai thao tác quản lý (liệt kê id, xoá sạch) để tải lại toàn bộ chỉ mục.
public class TourVectorStore extends SimpleVectorStore {

    public TourVectorStore(EmbeddingModel embeddingModel) {
        super(SimpleVectorStore.builder(embeddingModel));
    }

    public Set<String> ids() {
        return Set.copyOf(store.keySet());
    }

    public void clearAll() {
        store.clear();
    }
}
