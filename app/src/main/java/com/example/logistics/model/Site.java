package com.example.logistics.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Entity
@Table(name = "site")
public class Site extends PanacheEntity {

    @Column(name = "site_code", nullable = false, unique = true, length = 50)
    public String siteCode;

    @Column(nullable = false)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String address;

    @ManyToOne
    @JoinColumn(name = "shipper_id", nullable = false)
    @JsonIgnoreProperties("sites")
    public Shipper shipper;

    public static List<Site> findByShipper(Long shipperId) {
        return list("shipper.id", shipperId);
    }

    public static Site findBySiteCode(String code) {
        return find("siteCode", code).firstResult();
    }
}
