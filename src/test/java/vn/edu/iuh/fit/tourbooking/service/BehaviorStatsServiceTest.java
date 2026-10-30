package vn.edu.iuh.fit.tourbooking.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BehaviorStatsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    @Test
    void default_range_is_last_30_days() {
        var range = BehaviorStatsService.normalize(null, null, TODAY);
        assertThat(range.to()).isEqualTo(TODAY);
        assertThat(range.from()).isEqualTo(TODAY.minusDays(29));
        assertThat(range.endExclusive()).isEqualTo(TODAY.plusDays(1).atStartOfDay());
    }

    @Test
    void reversed_range_is_swapped_and_span_is_capped() {
        var swapped = BehaviorStatsService.normalize(TODAY, TODAY.minusDays(3), TODAY);
        assertThat(swapped.from()).isEqualTo(TODAY.minusDays(3));
        assertThat(swapped.to()).isEqualTo(TODAY);

        var capped = BehaviorStatsService.normalize(TODAY.minusYears(5), TODAY, TODAY);
        assertThat(capped.from()).isEqualTo(TODAY.minusDays(364));
    }

    @Test
    void funnel_percentages_are_relative_to_the_widest_step_and_safe_on_zero() {
        var steps = BehaviorStatsService.funnel(200, 50, 10);
        assertThat(steps).extracting(BehaviorStatsService.FunnelStep::percentOfTop).containsExactly(100, 25, 5);
        assertThat(BehaviorStatsService.funnel(0, 0, 0))
                .extracting(BehaviorStatsService.FunnelStep::percentOfTop).containsExactly(0, 0, 0);
    }

    @Test
    void tour_rates_never_divide_by_zero() {
        var row = new BehaviorStatsService.TourFunnelRow(1L, "A", 100, 40, 10, 5);
        assertThat(row.clickRate()).isEqualTo(0.4);
        assertThat(row.cartRate()).isEqualTo(0.1);
        assertThat(row.bookingRate()).isEqualTo(0.5);
        assertThat(new BehaviorStatsService.TourFunnelRow(2L, "B", 0, 0, 0, 0).bookingRate()).isZero();
    }
}
