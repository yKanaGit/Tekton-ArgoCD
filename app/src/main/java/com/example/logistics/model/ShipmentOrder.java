package com.example.logistics.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_order")
public class ShipmentOrder extends PanacheEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 100)
    public String orderNumber;

    @ManyToOne
    @JoinColumn(name = "shipper_id", nullable = false)
    public Shipper shipper;

    @ManyToOne
    @JoinColumn(name = "origin_site_id", nullable = false)
    public Site originSite;

    @ManyToOne
    @JoinColumn(name = "destination_site_id", nullable = false)
    public Site destinationSite;

    @Column(name = "item_description", columnDefinition = "TEXT")
    public String itemDescription;

    public Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    public IntegrationStatus status;

    @Column(name = "correlation_id", length = 100)
    public String correlationId;

    @Column(name = "created_at")
    public LocalDateTime createdAt;
}
