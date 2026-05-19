package com.example.logistics.integration;

import java.time.LocalDateTime;

public class IntegrationResponse {
    public String correlationId;
    public String shipperCode;
    public String siteCode;
    public String channel;
    public String status;
    public LocalDateTime receivedAt;
    public String message;

    public IntegrationResponse() {
    }

    public IntegrationResponse(String correlationId, String shipperCode, String siteCode,
                              String channel, String status, LocalDateTime receivedAt, String message) {
        this.correlationId = correlationId;
        this.shipperCode = shipperCode;
        this.siteCode = siteCode;
        this.channel = channel;
        this.status = status;
        this.receivedAt = receivedAt;
        this.message = message;
    }
}
