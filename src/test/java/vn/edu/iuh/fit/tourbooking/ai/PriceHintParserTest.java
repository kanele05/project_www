package vn.edu.iuh.fit.tourbooking.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PriceHintParserTest {

    @Test
    void under_million_phrase_becomes_max_price_and_is_removed_from_query() {
        var hint = PriceHintParser.parse("biển yên tĩnh cho gia đình dưới 5 triệu");
        assertThat(hint.maxPrice()).isEqualTo(5_000_000L);
        assertThat(hint.minPrice()).isNull();
        assertThat(hint.cleanedQuery()).isEqualTo("biển yên tĩnh cho gia đình");
    }

    @Test
    void abbreviations_and_decimals_are_understood() {
        assertThat(PriceHintParser.parse("tour đà lạt tối đa 3tr").maxPrice()).isEqualTo(3_000_000L);
        assertThat(PriceHintParser.parse("không quá 2,5 triệu").maxPrice()).isEqualTo(2_500_000L);
        assertThat(PriceHintParser.parse("dưới 4.500.000").maxPrice()).isEqualTo(4_500_000L);
        assertThat(PriceHintParser.parse("duoi 800 nghin").maxPrice()).isEqualTo(800_000L);
    }

    @Test
    void range_sets_both_bounds_and_borrows_the_unit() {
        var hint = PriceHintParser.parse("tour biển từ 3 đến 6 triệu");
        assertThat(hint.minPrice()).isEqualTo(3_000_000L);
        assertThat(hint.maxPrice()).isEqualTo(6_000_000L);
        assertThat(hint.cleanedQuery()).isEqualTo("tour biển");

        var thousands = PriceHintParser.parse("từ 500 đến 800 nghìn");
        assertThat(thousands.minPrice()).isEqualTo(500_000L);
        assertThat(thousands.maxPrice()).isEqualTo(800_000L);
    }

    @Test
    void min_price_phrase() {
        var hint = PriceHintParser.parse("resort trên 4 triệu");
        assertThat(hint.minPrice()).isEqualTo(4_000_000L);
        assertThat(hint.maxPrice()).isNull();
    }

    @Test
    void query_without_price_is_untouched() {
        var hint = PriceHintParser.parse("núi rừng săn mây");
        assertThat(hint.hasPrice()).isFalse();
        assertThat(hint.cleanedQuery()).isEqualTo("núi rừng săn mây");
    }

    @Test
    void blank_or_null_is_safe() {
        assertThat(PriceHintParser.parse(null).cleanedQuery()).isEmpty();
        assertThat(PriceHintParser.parse("   ").hasPrice()).isFalse();
    }
}
