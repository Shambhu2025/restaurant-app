package com.impactics.restaurant_app.repository;

import com.impactics.restaurant_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}