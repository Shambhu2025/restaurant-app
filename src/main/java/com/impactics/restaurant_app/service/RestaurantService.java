package com.impactics.restaurant_app.service;

import com.impactics.restaurant_app.dto.CreateRestaurantRequest;
import com.impactics.restaurant_app.dto.RestaurantResponse;
import com.impactics.restaurant_app.entity.Restaurant;
import com.impactics.restaurant_app.exception.ResourceNotFoundException;
import com.impactics.restaurant_app.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import com.impactics.restaurant_app.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @Cacheable(value = CacheConfig.RESTAURANTS_CACHE, key = "'all'")
    public List<RestaurantResponse> getAllRestaurants() {
        log.info("Cache MISS - fetching all active restaurants from database");
        return restaurantRepository.findByIsActiveTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RestaurantResponse getRestaurantById(UUID id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
        return toResponse(restaurant);
    }

    @CacheEvict(value = CacheConfig.RESTAURANTS_CACHE, key = "'all'")
    public RestaurantResponse createRestaurant(CreateRestaurantRequest request) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setCuisine(request.getCuisine());
        restaurant.setDeliveryFee(request.getDeliveryFee());
        restaurant.setMinOrderAmount(request.getMinOrderAmount());
        restaurant.setIsActive(true);

        Restaurant saved = restaurantRepository.save(restaurant);
        return toResponse(saved);
    }

    private RestaurantResponse toResponse(Restaurant restaurant) {
        RestaurantResponse response = new RestaurantResponse();
        response.setId(restaurant.getId());
        response.setName(restaurant.getName());
        response.setDescription(restaurant.getDescription());
        response.setCuisine(restaurant.getCuisine());
        response.setDeliveryFee(restaurant.getDeliveryFee());
        response.setMinOrderAmount(restaurant.getMinOrderAmount());
        response.setIsActive(restaurant.getIsActive());
        response.setCreatedAt(restaurant.getCreatedAt());
        response.setUpdatedAt(restaurant.getUpdatedAt());
        return response;
    }
}