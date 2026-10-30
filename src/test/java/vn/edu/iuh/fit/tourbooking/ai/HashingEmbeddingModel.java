package vn.edu.iuh.fit.tourbooking.ai;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Mô hình nhúng giả cho kiểm thử: băm từng từ vào vector 256 chiều (túi từ), không cần mạng hay khoá API.
public class HashingEmbeddingModel implements EmbeddingModel {

    static final int DIMENSIONS = 256;

    public int calls = 0;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        calls++;
        List<Embedding> result = new ArrayList<>();
        int index = 0;
        for (String text : request.getInstructions()) {
            result.add(new Embedding(vector(text), index++));
        }
        return new EmbeddingResponse(result);
    }

    @Override
    public float[] embed(Document document) {
        return call(new EmbeddingRequest(List.of(document.getText()), null)).getResults().get(0).getOutput();
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    static float[] vector(String text) {
        float[] v = new float[DIMENSIONS];
        String plain = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd');
        for (String word : plain.split("[^a-z0-9]+")) {
            if (word.length() > 1) {
                v[Math.floorMod(word.hashCode(), DIMENSIONS)] += 1f;
            }
        }
        return v;
    }
}
