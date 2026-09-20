package com.impactics.restaurant_app.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class MenuItemResponse {

    private UUID id;
    private UUID restaurantId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer calories;
    private Boolean isAvailable;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}