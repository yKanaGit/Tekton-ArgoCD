package com.example.logistics.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipper")
public class Shipper extends PanacheEntity {

    @Column(name = "shipper_code", nullable = false, unique = true, length = 50)
    public String shipperCode;

    @Column(nullable = false)
    public String name;

    @Column(name = "contact_name")
    public String contactName;

    @Column(name = "contact_email")
    public String contactEmail;

    @Column(name = "contact_phone", length = 50)
    public String contactPhone;

    @OneToMany(mappedBy = "shipper", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("shipper")
    public List<Site> sites = new ArrayList<>();

    public static Shipper findByShipperCode(String code) {
        return find("shipperCode", code).firstResult();
    }
}
