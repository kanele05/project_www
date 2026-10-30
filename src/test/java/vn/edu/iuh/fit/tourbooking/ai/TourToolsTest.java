package vn.edu.iuh.fit.tourbooking.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpSession;
import vn.edu.iuh.fit.tourbooking.entity.Tour;
import vn.edu.iuh.fit.tourbooking.entity.TourDeparture;
import vn.edu.iuh.fit.tourbooking.exception.BusinessRuleException;
import vn.edu.iuh.fit.tourbooking.service.CartService;
import vn.edu.iuh.fit.tourbooking.service.TourService;
import vn.edu.iuh.fit.tourbooking.service.TrackingService;
import vn.edu.iuh.fit.tourbooking.session.CartItem;
import vn.edu.iuh.fit.tourbooking.util.MessageHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourToolsTest {

    private SemanticSearchService search;
    private TourService tourService;
    private CartService cartService;
    private TrackingService tracking;
    private TourTools tools;

    @BeforeEach
    void setUp() {
        search = mock(SemanticSearchService.class);
        tourService = mock(TourService.class);
        cartService = mock(CartService.class);
        tracking = mock(TrackingService.class);
        var source = new StaticMessageSource();
        source.addMessage("error.cart.notBookable", java.util.Locale.getDefault(), "Lịch khởi hành này đã đóng bán.");
        source.setUseCodeAsDefaultMessage(true);
        tools = new TourTools(search, tourService, cartService, tracking, new MessageHelper(source));
    }

    @Test
    void search_tours_returns_compact_briefs_with_links() {
        Tour t = AiTestSupport.sampleTours().get(1);
        when(search.searchList("biển", 5_000_000L, 5)).thenReturn(List.of(t));

        var result = tools.searchTours("biển", 5_000_000L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).priceFromVnd()).isEqualTo(4_590_000L);
        assertThat(result.get(0).url()).isEqualTo("/tours/2/tour-2");
    }

    @Test
    void departures_tool_exposes_only_what_the_model_needs() {
        TourDeparture d = new TourDeparture();
        d.setId(9L);
        d.setDepartureDate(LocalDate.of(2026, 12, 1));
        d.setAvailableSeats(7);
        d.setPriceAdult(BigDecimal.valueOf(3_000_000));
        d.setPriceChild(BigDecimal.valueOf(1_500_000));
        when(tourService.findBookableDepartures(2L)).thenReturn(List.of(d));

        var result = tools.getDepartures(2L);

        assertThat(result).singleElement().satisfies(b -> {
            assertThat(b.departureId()).isEqualTo(9L);
            assertThat(b.seatsLeft()).isEqualTo(7);
            assertThat(b.priceChildVnd()).isEqualTo(1_500_000L);
        });
    }

    @Test
    void add_to_cart_goes_through_cart_service_and_records_tracking() {
        var session = new MockHttpSession();
        CartItem item = new CartItem();
        item.setTourId(2L);
        item.setDepartureId(9L);
        item.setTourName("Nha Trang");
        item.setNumAdults(2);
        item.setNumChildren(1);
        when(cartService.addByDeparture(session, 9L, 2, 1)).thenReturn(item);

        String reply = tools.addToCart(9L, 2, 1,
                new ToolContext(Map.of(TourTools.CTX_SESSION, session, TourTools.CTX_USER_ID, 77L)));

        assertThat(reply).contains("Đã thêm vào giỏ").contains("Nha Trang").contains("không thanh toán".substring(0, 0));
        verify(tracking).recordAddToCart(session.getId(), 77L, 2L, 9L);
    }

    @Test
    void add_to_cart_reports_business_rule_failures_as_text_and_never_tracks() {
        var session = new MockHttpSession();
        when(cartService.addByDeparture(session, 9L, 2, 0)).thenThrow(new BusinessRuleException("error.cart.notBookable"));

        String reply = tools.addToCart(9L, 2, 0, new ToolContext(Map.of(TourTools.CTX_SESSION, session)));

        assertThat(reply).startsWith("Không thêm được");
        verify(tracking, never()).recordAddToCart(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                anyLong(), anyLong());
    }

    @Test
    void add_to_cart_without_a_session_in_the_context_does_nothing() {
        String reply = tools.addToCart(9L, 1, 0, new ToolContext(Map.of()));
        assertThat(reply).contains("Không thêm được");
        verify(cartService, never()).addByDeparture(org.mockito.ArgumentMatchers.any(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void the_tool_class_exposes_no_checkout_or_payment_method() {
        assertThat(java.util.Arrays.stream(TourTools.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(org.springframework.ai.tool.annotation.Tool.class))
                .map(java.lang.reflect.Method::getName))
                .containsExactlyInAnyOrder("searchTours", "getDepartures", "addToCart");
    }
}
