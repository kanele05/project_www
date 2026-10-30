package vn.edu.iuh.fit.tourbooking.ai;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

// Bọc một EmbeddingModel và giãn cách tối thiểu giữa hai lời gọi (SimpleVectorStore gọi API từng tài liệu một) để không vượt hạn mức miễn phí.
public class ThrottledEmbeddingModel implements EmbeddingModel {

    private final EmbeddingModel delegate;
    private final long minIntervalMs;
    private final LongSupplier clock;
    private final LongConsumer sleeper;
    private long nextAllowedAt;

    public ThrottledEmbeddingModel(EmbeddingModel delegate, long minIntervalMs) {
        this(delegate, minIntervalMs, System::currentTimeMillis, ThrottledEmbeddingModel::sleep);
    }

    ThrottledEmbeddingModel(EmbeddingModel delegate, long minIntervalMs, LongSupplier clock, LongConsumer sleeper) {
        this.delegate = delegate;
        this.minIntervalMs = minIntervalMs;
        this.clock = clock;
        this.sleeper = sleeper;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        throttle();
        return delegate.call(request);
    }

    @Override
    public float[] embed(Document document) {
        throttle();
        return delegate.embed(document);
    }

    @Override
    public int dimensions() {
        return delegate.dimensions();
    }

    private void throttle() {
        if (minIntervalMs <= 0) {
            return;
        }
        long wait;
        synchronized (this) {
            long now = clock.getAsLong();
            long start = Math.max(now, nextAllowedAt);
            wait = start - now;
            nextAllowedAt = start + minIntervalMs;
        }
        if (wait > 0) {
            sleeper.accept(wait);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
