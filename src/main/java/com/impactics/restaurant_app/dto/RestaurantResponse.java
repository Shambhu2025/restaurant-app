package com.impactics.restaurant_app.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class RestaurantResponse {

    private UUID id;
    private String name;
    private String description;
    private String cuisine;
    private BigDecimal deliveryFee;
    private BigDecimal minOrderAmount;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}