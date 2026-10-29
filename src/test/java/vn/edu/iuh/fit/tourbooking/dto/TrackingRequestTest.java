package vn.edu.iuh.fit.tourbooking.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vn.edu.iuh.fit.tourbooking.dto.form.TrackingRequest;
import vn.edu.iuh.fit.tourbooking.entity.EventType;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingRequestTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void view_needs_a_tour() {
        assertThat(validator.validate(new TrackingRequest(EventType.VIEW, 5L, null, null, null, "/tours/5"))).isEmpty();
        assertThat(validator.validate(new TrackingRequest(EventType.VIEW, null, null, null, null, null))).isNotEmpty();
    }

    @Test
    void search_needs_a_keyword() {
        assertThat(validator.validate(new TrackingRequest(EventType.SEARCH, null, null, "đà lạt", null, null))).isEmpty();
        assertThat(validator.validate(new TrackingRequest(EventType.SEARCH, null, null, "  ", null, null))).isNotEmpty();
    }

    @Test
    void time_on_page_needs_tour_and_duration() {
        assertThat(validator.validate(new TrackingRequest(EventType.TIME_ON_PAGE, 1L, null, null, 4000, null))).isEmpty();
        assertThat(validator.validate(new TrackingRequest(EventType.TIME_ON_PAGE, 1L, null, null, null, null))).isNotEmpty();
    }

    @Test
    void rejects_negative_ids_oversized_duration_and_long_keyword() {
        assertThat(validator.validate(new TrackingRequest(EventType.CLICK, -1L, null, null, null, null))).isNotEmpty();
        assertThat(validator.validate(new TrackingRequest(EventType.TIME_ON_PAGE, 1L, null, null, 90_000_000, null))).isNotEmpty();
        assertThat(validator.validate(new TrackingRequest(EventType.SEARCH, null, null, "x".repeat(201), null, null))).isNotEmpty();
    }

    @Test
    void type_is_required() {
        assertThat(validator.validate(new TrackingRequest(null, 1L, null, null, null, null))).isNotEmpty();
    }
}
