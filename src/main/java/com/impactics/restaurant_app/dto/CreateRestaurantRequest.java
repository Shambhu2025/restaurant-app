package com.impactics.restaurant_app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Payload for creating a new restaurant")
public class CreateRestaurantRequest {

    @NotBlank(message = "name is required")
    @Schema(description = "The name of the restaurant", example = "Spicy Palace")
    private String name;

    @Schema(description = "Brief description of the restaurant", example = "Authentic Indian cuisine")
    private String description;

    @Schema(description = "Primary cuisine type", example = "Indian")
    private String cuisine;

    @Schema(description = "Base delivery fee", example = "2.99")
    private BigDecimal deliveryFee;

    @Schema(description = "Minimum order amount required for delivery", example = "15.00")
    private BigDecimal minOrderAmount;
}