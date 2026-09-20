package com.impactics.restaurant_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "password is required")
    private String password;

    private String firstName;

    private String lastName;

    private String phone;
}