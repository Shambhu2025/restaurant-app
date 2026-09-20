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
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    private BigDecimal subtotal;

    @Column(name = "delivery_fee")
    private BigDecimal deliveryFee;

    private BigDecimal total;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "delivery_notes")
    private String deliveryNotes;

    private String status;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;
}