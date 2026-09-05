package com.impactics.restaurant_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impactics.restaurant_app.dto.CreateRestaurantRequest;
import com.impactics.restaurant_app.dto.RestaurantResponse;
import com.impactics.restaurant_app.exception.ResourceNotFoundException;
import com.impactics.restaurant_app.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RestaurantController.class)
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RestaurantService restaurantService;

    @Test
    void getAllRestaurants_returnsOkWithList() throws Exception {
        RestaurantResponse response = new RestaurantResponse();
        response.setId(UUID.randomUUID());
        response.setName("Pizza Palace");

        when(restaurantService.getAllRestaurants()).thenReturn(List.of(response));

        mockMvc.perform(get("/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pizza Palace"));
    }

    @Test
    void getRestaurantById_whenExists_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        RestaurantResponse response = new RestaurantResponse();
        response.setId(id);
        response.setName("Pizza Palace");

        when(restaurantService.getRestaurantById(id)).thenReturn(response);

        mockMvc.perform(get("/restaurants/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pizza Palace"));
    }

    @Test
    void getRestaurantById_whenNotFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(restaurantService.getRestaurantById(id))
                .thenThrow(new ResourceNotFoundException("Restaurant not found with id: " + id));

        mockMvc.perform(get("/restaurants/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void createRestaurant_withValidData_returns201() throws Exception {
        CreateRestaurantRequest request = new CreateRestaurantRequest();
        request.setName("Burger Barn");
        request.setDescription("Great burgers");
        request.setCuisine("American");
        request.setDeliveryFee(new BigDecimal("1.99"));
        request.setMinOrderAmount(new BigDecimal("8.00"));

        RestaurantResponse response = new RestaurantResponse();
        response.setId(UUID.randomUUID());
        response.setName("Burger Barn");

        when(restaurantService.createRestaurant(any(CreateRestaurantRequest.class))).thenReturn(response);

        mockMvc.perform(post("/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Burger Barn"));
    }

    @Test
    void createRestaurant_withMissingName_returns400() throws Exception {
        CreateRestaurantRequest request = new CreateRestaurantRequest();
        request.setDescription("Missing name field");

        mockMvc.perform(post("/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}