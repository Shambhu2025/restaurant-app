package com.impactics.restaurant_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impactics.restaurant_app.dto.CreateUserRequest;
import com.impactics.restaurant_app.dto.UserResponse;
import com.impactics.restaurant_app.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @Test
    void createUser_withValidData_returns201() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("john@example.com");
        request.setPassword("secret123");
        request.setFirstName("John");
        request.setLastName("Doe");

        UserResponse response = new UserResponse();
        response.setId(UUID.randomUUID());
        response.setEmail("john@example.com");
        response.setFirstName("John");
        response.setStatus("ACTIVE");

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void createUser_withInvalidEmail_returns400() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("not-an-email");
        request.setPassword("secret123");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_withMissingPassword_returns400() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("john@example.com");
        // password intentionally omitted

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}