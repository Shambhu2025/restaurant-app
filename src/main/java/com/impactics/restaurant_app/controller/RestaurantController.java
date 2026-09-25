package com.impactics.restaurant_app.controller;

import com.impactics.restaurant_app.dto.CreateRestaurantRequest;
import com.impactics.restaurant_app.dto.RestaurantResponse;
import com.impactics.restaurant_app.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/restaurants")
@Tag(name = "Restaurant Management", description = "Endpoints for managing restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping
    @Operation(summary = "Get all restaurants", description = "Retrieves a list of all active restaurants.")
    public ResponseEntity<List<RestaurantResponse>> getAllRestaurants() {
        return ResponseEntity.ok(restaurantService.getAllRestaurants());
    }

    @GetMapping("/{restaurantId}")
    @Operation(summary = "Get a restaurant by ID", description = "Retrieves details of a specific restaurant.")
    public ResponseEntity<RestaurantResponse> getRestaurantById(@PathVariable UUID restaurantId) {
        return ResponseEntity.ok(restaurantService.getRestaurantById(restaurantId));
    }

    @PostMapping
    @Operation(summary = "Create a new restaurant", description = "Registers a new restaurant in the system.")
    public ResponseEntity<RestaurantResponse> createRestaurant(@Valid @RequestBody CreateRestaurantRequest request) {
        RestaurantResponse created = restaurantService.createRestaurant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}