package vn.edu.iuh.fit.tourbooking.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

// Điều kiện nạp các bean AI: app.ai.enabled=true, hoặc để trống mà có app.ai.api-key.
public class AiEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return AiProperties.resolveEnabled(
                context.getEnvironment().getProperty("app.ai.enabled"),
                context.getEnvironment().getProperty("app.ai.api-key"));
    }
}
