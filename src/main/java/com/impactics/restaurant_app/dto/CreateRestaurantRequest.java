package com.impactics.restaurant_app.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class CreateRestaurantRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    private String cuisine;

    private BigDecimal deliveryFee;

    private BigDecimal minOrderAmount;

    // Getters and setters

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
}