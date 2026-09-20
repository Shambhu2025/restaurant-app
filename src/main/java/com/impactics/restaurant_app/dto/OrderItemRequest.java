package com.impactics.restaurant_app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class OrderItemRequest {

    @NotNull(message = "menuItemId is required")
    private UUID menuItemId;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than 0")
    private Integer quantity;

    private String specialInstructions;
}