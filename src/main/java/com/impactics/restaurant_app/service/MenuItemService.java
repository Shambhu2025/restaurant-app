package com.impactics.restaurant_app.service;

import com.impactics.restaurant_app.dto.CreateMenuItemRequest;
import com.impactics.restaurant_app.dto.MenuItemResponse;
import com.impactics.restaurant_app.entity.MenuItem;
import com.impactics.restaurant_app.entity.Restaurant;
import com.impactics.restaurant_app.exception.ResourceNotFoundException;
import com.impactics.restaurant_app.repository.MenuItemRepository;
import com.impactics.restaurant_app.repository.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    public MenuItemService(MenuItemRepository menuItemRepository, RestaurantRepository restaurantRepository) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public List<MenuItemResponse> getMenuByRestaurantId(UUID restaurantId) {
        // Ensure the restaurant actually exists before querying its menu
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant not found with id: " + restaurantId);
        }

        return menuItemRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MenuItemResponse createMenuItem(UUID restaurantId, CreateMenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurant(restaurant);
        menuItem.setName(request.getName());
        menuItem.setDescription(request.getDescription());
        menuItem.setPrice(request.getPrice());
        menuItem.setCalories(request.getCalories());
        menuItem.setIsAvailable(true);

        MenuItem saved = menuItemRepository.save(menuItem);
        return toResponse(saved);
    }

    private MenuItemResponse toResponse(MenuItem menuItem) {
        MenuItemResponse response = new MenuItemResponse();
        response.setId(menuItem.getId());
        response.setRestaurantId(menuItem.getRestaurant().getId());
        response.setName(menuItem.getName());
        response.setDescription(menuItem.getDescription());
        response.setPrice(menuItem.getPrice());
        response.setCalories(menuItem.getCalories());
        response.setIsAvailable(menuItem.getIsAvailable());
        response.setCreatedAt(menuItem.getCreatedAt());
        response.setUpdatedAt(menuItem.getUpdatedAt());
        return response;
    }
}