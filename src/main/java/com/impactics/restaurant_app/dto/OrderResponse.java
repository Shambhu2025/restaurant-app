package com.impactics.restaurant_app.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OrderResponse {

    private UUID id;
    private UUID userId;
    private UUID restaurantId;
    private List<OrderItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal total;
    private String deliveryAddress;
    private String deliveryNotes;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}