package vn.edu.iuh.fit.tourbooking.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class AiPropertiesTest {

    @Test
    void explicit_flag_wins_otherwise_the_api_key_decides() {
        assertThat(AiProperties.resolveEnabled(null, null)).isFalse();
        assertThat(AiProperties.resolveEnabled("", "")).isFalse();
        assertThat(AiProperties.resolveEnabled("", "AIza-abc")).isTrue();
        assertThat(AiProperties.resolveEnabled("false", "AIza-abc")).isFalse();
        assertThat(AiProperties.resolveEnabled("true", "")).isTrue();
    }

    @Test
    void condition_reads_the_environment_the_same_way() {
        var condition = new AiEnabledCondition();
        var off = new MockEnvironment().withProperty("app.ai.api-key", "").withProperty("app.ai.enabled", "");
        var on = new MockEnvironment().withProperty("app.ai.api-key", "AIza-abc");
        var forcedOff = new MockEnvironment().withProperty("app.ai.api-key", "AIza-abc")
                .withProperty("app.ai.enabled", "false");

        assertThat(condition.matches(contextOf(off), null)).isFalse();
        assertThat(condition.matches(contextOf(on), null)).isTrue();
        assertThat(condition.matches(contextOf(forcedOff), null)).isFalse();
    }

    private static org.springframework.context.annotation.ConditionContext contextOf(MockEnvironment env) {
        var ctx = org.mockito.Mockito.mock(org.springframework.context.annotation.ConditionContext.class);
        org.mockito.Mockito.when(ctx.getEnvironment()).thenReturn(env);
        return ctx;
    }
}
