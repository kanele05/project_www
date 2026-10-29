package vn.edu.iuh.fit.tourbooking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.tourbooking.config.TrackingProperties;
import vn.edu.iuh.fit.tourbooking.dto.form.TrackingRequest;
import vn.edu.iuh.fit.tourbooking.entity.EventType;
import vn.edu.iuh.fit.tourbooking.entity.UserEvent;
import vn.edu.iuh.fit.tourbooking.exception.RateLimitExceededException;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserEventRepository;
import vn.edu.iuh.fit.tourbooking.util.SlidingWindowRateLimiter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Service
@Slf4j
// Ghi sự kiện hành vi: kiểm tra, cắt độ dài, băm session, lọc bot, giới hạn tốc độ; lỗi ghi không bao giờ làm hỏng nghiệp vụ chính.
public class TrackingService {

    public enum Result { RECORDED, IGNORED }

    private static final Pattern BOT_AGENT = Pattern.compile(
            "bot|crawl|spider|slurp|headless|lighthouse|curl|wget|python-requests|httpclient|monitor",
            Pattern.CASE_INSENSITIVE);

    private static final int MAX_KEYWORD = 200;
    private static final int MAX_PAGE_URL = 255;

    private final UserEventRepository eventRepository;
    private final TourRepository tourRepository;
    private final TrackingProperties properties;
    private final SlidingWindowRateLimiter limiter;

    public TrackingService(UserEventRepository eventRepository, TourRepository tourRepository,
                           TrackingProperties properties) {
        this.eventRepository = eventRepository;
        this.tourRepository = tourRepository;
        this.properties = properties;
        this.limiter = new SlidingWindowRateLimiter(properties.maxEventsPerMinute(), Duration.ofMinutes(1));
    }

    // Ghi một sự kiện do trình duyệt gửi; userId lấy từ SecurityContext, sessionId từ HttpSession (không tin giá trị client).
    @Transactional
    public Result record(TrackingRequest request, String sessionId, Long userId, String userAgent) {
        if (!properties.enabled() || isBot(userAgent) || sessionId == null) {
            return Result.IGNORED;
        }
        String sessionKey = hashSession(sessionId);
        if (!limiter.tryAcquire(sessionKey)) {
            throw new RateLimitExceededException("error.tracking.rateLimited");
        }

        Integer duration = request.durationMs();
        if (request.type() == EventType.TIME_ON_PAGE) {
            if (duration == null || duration < properties.minDurationMs()) {
                return Result.IGNORED;
            }
            duration = Math.min(duration, properties.maxDurationMs());
        } else {
            duration = null;
        }

        if (request.tourId() != null && !tourRepository.existsById(request.tourId())) {
            return Result.IGNORED;
        }

        UserEvent event = new UserEvent();
        event.setType(request.type());
        event.setSessionId(sessionKey);
        event.setUserId(userId);
        event.setTourId(request.tourId());
        event.setDepartureId(request.departureId());
        event.setKeyword(request.type() == EventType.SEARCH ? cleanKeyword(request.keyword()) : null);
        event.setDurationMs(duration);
        event.setPageUrl(cleanPath(request.pageUrl()));
        return save(event);
    }

    // Ghi sự kiện ADD_TO_CART ở phía server sau khi thêm giỏ thành công (chính xác hơn bắt submit ở trình duyệt).
    public void recordAddToCart(String sessionId, Long userId, Long tourId, Long departureId) {
        if (!properties.enabled() || sessionId == null || tourId == null) {
            return;
        }
        UserEvent event = new UserEvent();
        event.setType(EventType.ADD_TO_CART);
        event.setSessionId(hashSession(sessionId));
        event.setUserId(userId);
        event.setTourId(tourId);
        event.setDepartureId(departureId);
        save(event);
    }

    // Dọn sự kiện quá hạn lưu trữ (mỗi đêm 03:15).
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void purgeExpired() {
        int deleted = eventRepository.deleteOlderThan(
                LocalDateTime.now().minusDays(properties.retentionDays()));
        if (deleted > 0) {
            log.info("Đã xoá {} sự kiện hành vi quá {} ngày", deleted, properties.retentionDays());
        }
    }

    private Result save(UserEvent event) {
        try {
            eventRepository.save(event);
            return Result.RECORDED;
        } catch (DataAccessException e) {
            log.warn("Không ghi được sự kiện {}: {}", event.getType(), e.getMessage());
            return Result.IGNORED;
        }
    }

    // Trình thu thập/tự động hoá không có hành vi người thật nên bị bỏ qua.
    public static boolean isBot(String userAgent) {
        return userAgent == null || userAgent.isBlank() || BOT_AGENT.matcher(userAgent).find();
    }

    // Băm SHA-256 mã phiên để không lưu mã phiên đang sống vào CSDL (64 ký tự hex).
    public static String hashSession(String sessionId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // Chuẩn hoá từ khoá: bỏ ký tự điều khiển, gộp khoảng trắng, cắt tối đa 200 ký tự; rỗng thì trả null.
    public static String cleanKeyword(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.length() > MAX_KEYWORD ? cleaned.substring(0, MAX_KEYWORD) : cleaned;
    }

    // Chỉ giữ phần đường dẫn (bỏ query và fragment để không lưu từ khoá/mã cá nhân), phải bắt đầu bằng "/".
    public static String cleanPath(String raw) {
        if (raw == null || !raw.startsWith("/")) {
            return null;
        }
        int cut = raw.length();
        for (char c : new char[]{'?', '#'}) {
            int i = raw.indexOf(c);
            if (i >= 0) {
                cut = Math.min(cut, i);
            }
        }
        String path = raw.substring(0, cut);
        return path.length() > MAX_PAGE_URL ? path.substring(0, MAX_PAGE_URL) : path;
    }
}
