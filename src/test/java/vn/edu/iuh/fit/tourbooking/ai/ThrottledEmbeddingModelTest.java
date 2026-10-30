package vn.edu.iuh.fit.tourbooking.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class ThrottledEmbeddingModelTest {

    @Test
    void spaces_consecutive_calls_by_the_minimum_interval() {
        AtomicLong now = new AtomicLong(1_000);
        List<Long> sleeps = new ArrayList<>();
        var inner = new HashingEmbeddingModel();
        var model = new ThrottledEmbeddingModel(inner, 700, now::get, ms -> {
            sleeps.add(ms);
            now.addAndGet(ms);
        });

        model.embed(new Document("một"));
        model.embed(new Document("hai"));
        model.embed(new Document("ba"));

        assertThat(inner.calls).isEqualTo(3);
        assertThat(sleeps).containsExactly(700L, 700L);
    }

    @Test
    void no_wait_when_calls_are_already_far_apart_or_interval_is_zero() {
        AtomicLong now = new AtomicLong(0);
        List<Long> sleeps = new ArrayList<>();
        var model = new ThrottledEmbeddingModel(new HashingEmbeddingModel(), 700, now::get, sleeps::add);
        model.embed(new Document("a"));
        now.set(5_000);
        model.embed(new Document("b"));
        assertThat(sleeps).isEmpty();

        var off = new ThrottledEmbeddingModel(new HashingEmbeddingModel(), 0, now::get, sleeps::add);
        off.embed(new Document("a"));
        off.embed(new Document("b"));
        assertThat(sleeps).isEmpty();
    }
}
