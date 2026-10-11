package com.impactics.restaurant_app.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class OrderEvent {

    private UUID orderId;
    private UUID idempotencyKey;
    private UUID userId;
    private UUID restaurantId;
    private List<OrderItemRequest> items;
    private String deliveryAddress;
    private String deliveryNotes;
}