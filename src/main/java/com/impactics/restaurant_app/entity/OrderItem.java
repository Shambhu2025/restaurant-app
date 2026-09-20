package com.impactics.restaurant_app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    private Integer quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "special_instructions")
    private String specialInstructions;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;
}