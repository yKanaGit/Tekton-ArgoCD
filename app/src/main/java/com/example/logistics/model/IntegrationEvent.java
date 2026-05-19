package com.example.logistics.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "integration_event")
public class IntegrationEvent extends PanacheEntity {

    @Column(name = "correlation_id", nullable = false, unique = true, length = 100)
    public String correlationId;

    @Column(name = "shipper_code", nullable = false, length = 50)
    public String shipperCode;

    @Column(name = "site_code", length = 50)
    public String siteCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public IntegrationStatus status;

    @Column(name = "received_at", nullable = false)
    public LocalDateTime receivedAt;

    @Column(name = "processed_at")
    public LocalDateTime processedAt;

    @Column(columnDefinition = "TEXT")
    public String message;

    @Column(columnDefinition = "TEXT")
    public String payload;

    public static IntegrationEvent findByCorrelationId(String correlationId) {
        return find("correlationId", correlationId).firstResult();
    }

    public static List<IntegrationEvent> findByShipperCode(String shipperCode) {
        return list("shipperCode", shipperCode);
    }
}
