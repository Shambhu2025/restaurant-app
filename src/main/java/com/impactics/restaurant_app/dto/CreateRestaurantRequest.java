package com.impactics.restaurant_app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateRestaurantRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    private String cuisine;

    private BigDecimal deliveryFee;

    private BigDecimal minOrderAmount;
}