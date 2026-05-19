package com.example.logistics.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "integration_partner")
public class IntegrationPartner extends PanacheEntity {

    @Column(name = "partner_code", nullable = false, unique = true, length = 50)
    public String partnerCode;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public PartnerType type;

    public static IntegrationPartner findByPartnerCode(String code) {
        return find("partnerCode", code).firstResult();
    }
}
