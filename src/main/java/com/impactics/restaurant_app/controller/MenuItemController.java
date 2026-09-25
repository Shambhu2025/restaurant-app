package com.impactics.restaurant_app.controller;

import com.impactics.restaurant_app.dto.CreateMenuItemRequest;
import com.impactics.restaurant_app.dto.MenuItemResponse;
import com.impactics.restaurant_app.service.MenuItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Menu Management", description = "Endpoints for managing menus and menu items")
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @PostMapping("/restaurants/{restaurantId}/menu-items")
    @Operation(summary = "Add a menu item", description = "Adds a new menu item to a specific restaurant.")
    public ResponseEntity<MenuItemResponse> createMenuItem(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody CreateMenuItemRequest request) {
        MenuItemResponse created = menuItemService.createMenuItem(restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/menu")
    @Operation(summary = "Get restaurant menu", description = "Retrieves all available menu items for a specific restaurant.")
    public ResponseEntity<List<MenuItemResponse>> getMenuByRestaurant(
            @RequestParam UUID restaurantId) {
        return ResponseEntity.ok(menuItemService.getMenuByRestaurantId(restaurantId));
    }
}