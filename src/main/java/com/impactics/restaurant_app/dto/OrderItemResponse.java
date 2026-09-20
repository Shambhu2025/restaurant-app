package com.impactics.restaurant_app.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemResponse {

    private UUID id;
    private UUID menuItemId;
    private String menuItemName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String specialInstructions;
}