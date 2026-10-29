package vn.edu.iuh.fit.tourbooking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import vn.edu.iuh.fit.tourbooking.config.TrackingProperties;
import vn.edu.iuh.fit.tourbooking.dto.form.TrackingRequest;
import vn.edu.iuh.fit.tourbooking.entity.EventType;
import vn.edu.iuh.fit.tourbooking.entity.UserEvent;
import vn.edu.iuh.fit.tourbooking.exception.RateLimitExceededException;
import vn.edu.iuh.fit.tourbooking.repository.TourRepository;
import vn.edu.iuh.fit.tourbooking.repository.UserEventRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrackingServiceTest {

    private static final String BROWSER = "Mozilla/5.0 (Windows NT 10.0) Chrome/126.0";

    private UserEventRepository events;
    private TourRepository tours;
    private TrackingService service;

    @BeforeEach
    void setUp() {
        events = mock(UserEventRepository.class);
        tours = mock(TourRepository.class);
        when(tours.existsById(5L)).thenReturn(true);
        service = new TrackingService(events, tours, new TrackingProperties(true, 3, 500, 1_800_000, 180));
    }

    private UserEvent saved() {
        ArgumentCaptor<UserEvent> captor = ArgumentCaptor.forClass(UserEvent.class);
        verify(events).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void stores_server_side_identity_and_hashes_the_session() {
        var result = service.record(new TrackingRequest(EventType.VIEW, 5L, null, null, 999, "/tours/5/x?utm=1"),
                "JSESSION-123", 42L, BROWSER);

        assertThat(result).isEqualTo(TrackingService.Result.RECORDED);
        UserEvent e = saved();
        assertThat(e.getUserId()).isEqualTo(42L);
        assertThat(e.getSessionId()).hasSize(64).isEqualTo(TrackingService.hashSession("JSESSION-123"))
                .doesNotContain("JSESSION");
        assertThat(e.getPageUrl()).isEqualTo("/tours/5/x");
        assertThat(e.getDurationMs()).as("VIEW không mang thời lượng").isNull();
        assertThat(e.getOccurredAt()).isNotNull();
    }

    @Test
    void ignores_bots_and_missing_user_agent() {
        var req = new TrackingRequest(EventType.VIEW, 5L, null, null, null, null);
        assertThat(service.record(req, "s", null, "Googlebot/2.1")).isEqualTo(TrackingService.Result.IGNORED);
        assertThat(service.record(req, "s", null, "HeadlessChrome/120")).isEqualTo(TrackingService.Result.IGNORED);
        assertThat(service.record(req, "s", null, null)).isEqualTo(TrackingService.Result.IGNORED);
        verify(events, never()).save(any());
    }

    @Test
    void rate_limits_per_session_but_not_across_sessions() {
        var req = new TrackingRequest(EventType.VIEW, 5L, null, null, null, null);
        for (int i = 0; i < 3; i++) {
            service.record(req, "same", null, BROWSER);
        }
        assertThatThrownBy(() -> service.record(req, "same", null, BROWSER))
                .isInstanceOf(RateLimitExceededException.class);
        assertThat(service.record(req, "other", null, BROWSER)).isEqualTo(TrackingService.Result.RECORDED);
    }

    @Test
    void time_on_page_is_clamped_and_short_visits_are_dropped() {
        service.record(new TrackingRequest(EventType.TIME_ON_PAGE, 5L, null, null, 86_000_000, null), "a", null, BROWSER);
        assertThat(saved().getDurationMs()).isEqualTo(1_800_000);

        var other = mock(UserEventRepository.class);
        var svc = new TrackingService(other, tours, new TrackingProperties(true, 60, 500, 1_800_000, 180));
        assertThat(svc.record(new TrackingRequest(EventType.TIME_ON_PAGE, 5L, null, null, 100, null), "b", null, BROWSER))
                .isEqualTo(TrackingService.Result.IGNORED);
        verify(other, never()).save(any());
    }

    @Test
    void unknown_tour_is_not_recorded() {
        var result = service.record(new TrackingRequest(EventType.CLICK, 999L, null, null, null, null), "a", null, BROWSER);
        assertThat(result).isEqualTo(TrackingService.Result.IGNORED);
        verify(events, never()).save(any());
    }

    @Test
    void keyword_is_cleaned_only_for_search_events() {
        service.record(new TrackingRequest(EventType.SEARCH, null, null, "  biển \t\n  đẹp  ", null, null), "a", null, BROWSER);
        assertThat(saved().getKeyword()).isEqualTo("biển đẹp");
    }

    @Test
    void database_failure_never_propagates() {
        when(events.save(any())).thenThrow(new DataAccessResourceFailureException("db down"));
        var result = service.record(new TrackingRequest(EventType.VIEW, 5L, null, null, null, null), "a", null, BROWSER);
        assertThat(result).isEqualTo(TrackingService.Result.IGNORED);
    }

    @Test
    void disabled_tracking_records_nothing() {
        var off = new TrackingService(events, tours, new TrackingProperties(false, 60, 500, 1_800_000, 180));
        assertThat(off.record(new TrackingRequest(EventType.VIEW, 5L, null, null, null, null), "a", null, BROWSER))
                .isEqualTo(TrackingService.Result.IGNORED);
        off.recordAddToCart("a", null, 5L, 1L);
        verify(events, never()).save(any());
    }

    @Test
    void add_to_cart_is_recorded_server_side() {
        service.recordAddToCart("sess", 7L, 5L, 11L);
        UserEvent e = saved();
        assertThat(e.getType()).isEqualTo(EventType.ADD_TO_CART);
        assertThat(e.getTourId()).isEqualTo(5L);
        assertThat(e.getDepartureId()).isEqualTo(11L);
        assertThat(e.getUserId()).isEqualTo(7L);
    }

    @Test
    void helpers_clean_inputs() {
        assertThat(TrackingService.cleanKeyword("   ")).isNull();
        assertThat(TrackingService.cleanKeyword("a".repeat(300))).hasSize(200);
        assertThat(TrackingService.cleanPath("javascript:alert(1)")).isNull();
        assertThat(TrackingService.cleanPath("/tours?q=secret#frag")).isEqualTo("/tours");
        assertThat(TrackingService.isBot("Mozilla/5.0 (compatible; bingbot/2.0)")).isTrue();
        assertThat(TrackingService.isBot(BROWSER)).isFalse();
    }
}
