package com.example.logistics.health;

import com.example.logistics.model.Shipper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        try {
            long count = Shipper.count();
            return HealthCheckResponse.builder()
                    .name("Database")
                    .up()
                    .withData("shipperCount", count)
                    .build();
        } catch (Exception e) {
            return HealthCheckResponse.builder()
                    .name("Database")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
