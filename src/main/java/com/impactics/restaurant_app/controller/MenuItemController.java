package com.impactics.restaurant_app.controller;

import com.impactics.restaurant_app.dto.CreateMenuItemRequest;
import com.impactics.restaurant_app.dto.MenuItemResponse;
import com.impactics.restaurant_app.service.MenuItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @PostMapping("/restaurants/{restaurantId}/menu-items")
    public ResponseEntity<MenuItemResponse> createMenuItem(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody CreateMenuItemRequest request) {
        MenuItemResponse created = menuItemService.createMenuItem(restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/menu")
    public ResponseEntity<List<MenuItemResponse>> getMenuByRestaurant(
            @RequestParam UUID restaurantId) {
        return ResponseEntity.ok(menuItemService.getMenuByRestaurantId(restaurantId));
    }
}