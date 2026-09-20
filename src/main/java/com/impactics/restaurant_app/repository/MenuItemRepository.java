package com.impactics.restaurant_app.repository;

import com.impactics.restaurant_app.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByRestaurantId(UUID restaurantId);

    List<MenuItem> findByRestaurantIdAndIsAvailableTrue(UUID restaurantId);
}