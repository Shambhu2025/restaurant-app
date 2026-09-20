package com.impactics.restaurant_app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
public class Restaurant {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String cuisine;

    @Column(name = "delivery_fee")
    private BigDecimal deliveryFee;

    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount;

    @Column(name = "is_active")
    private Boolean isActive;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY)
    private List<MenuItem> menuItems;

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY)
    private List<Order> orders;
}