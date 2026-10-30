package vn.edu.iuh.fit.tourbooking.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class SlidingWindowRateLimiterTest {

    @Test
    void blocks_after_limit_and_frees_up_when_window_slides() {
        AtomicLong now = new AtomicLong(0);
        var limiter = new SlidingWindowRateLimiter(3, Duration.ofMinutes(1), now::get);

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();

        now.set(59_999);
        assertThat(limiter.tryAcquire("a")).isFalse();

        now.set(60_000);
        assertThat(limiter.tryAcquire("a")).isTrue();
    }

    @Test
    void keys_are_independent() {
        var limiter = new SlidingWindowRateLimiter(1, Duration.ofMinutes(1), () -> 0L);
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("b")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
    }
}
