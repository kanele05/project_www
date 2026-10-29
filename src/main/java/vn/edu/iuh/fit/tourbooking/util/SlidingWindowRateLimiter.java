package vn.edu.iuh.fit.tourbooking.util;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

// Giới hạn số lần gọi trong một cửa sổ thời gian trượt theo từng khoá (session), giữ trong bộ nhớ.
public class SlidingWindowRateLimiter {

    private static final int MAX_TRACKED_KEYS = 20_000;

    private final int maxCalls;
    private final long windowMillis;
    private final LongSupplier clock;
    private final Map<String, ArrayDeque<Long>> calls = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int maxCalls, Duration window) {
        this(maxCalls, window, System::currentTimeMillis);
    }

    public SlidingWindowRateLimiter(int maxCalls, Duration window, LongSupplier clock) {
        this.maxCalls = maxCalls;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    // Xin phép thực hiện một lần gọi; trả về false nếu khoá này đã dùng hết hạn mức trong cửa sổ.
    public boolean tryAcquire(String key) {
        long now = clock.getAsLong();
        if (calls.size() > MAX_TRACKED_KEYS) {
            evictExpired(now);
        }

        ArrayDeque<Long> window = calls.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= windowMillis) {
                window.pollFirst();
            }
            if (window.size() >= maxCalls) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    private void evictExpired(long now) {
        calls.entrySet().removeIf(entry -> {
            ArrayDeque<Long> window = entry.getValue();
            synchronized (window) {
                return window.isEmpty() || now - window.peekLast() >= windowMillis;
            }
        });
    }
}
