package com.impactics.restaurant_app.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class UserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}