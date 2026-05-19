package com.example.logistics.health;

import com.example.logistics.model.IntegrationEvent;
import com.example.logistics.model.IntegrationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class IntegrationHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        try {
            long totalEvents = IntegrationEvent.count();
            long failedEvents = IntegrationEvent.count("status", IntegrationStatus.FAILED);

            return HealthCheckResponse.builder()
                    .name("Integration Service")
                    .up()
                    .withData("totalEvents", totalEvents)
                    .withData("failedEvents", failedEvents)
                    .build();
        } catch (Exception e) {
            return HealthCheckResponse.builder()
                    .name("Integration Service")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
