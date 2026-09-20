package com.impactics.restaurant_app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "restaurantId is required")
    private UUID restaurantId;

    @NotEmpty(message = "order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    private String deliveryAddress;

    private String deliveryNotes;
}