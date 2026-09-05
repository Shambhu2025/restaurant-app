package com.impactics.restaurant_app.repository;

import com.impactics.restaurant_app.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}