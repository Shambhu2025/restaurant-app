package com.impactics.restaurant_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impactics.restaurant_app.dto.CreateMenuItemRequest;
import com.impactics.restaurant_app.dto.MenuItemResponse;
import com.impactics.restaurant_app.service.MenuItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MenuItemController.class)
class MenuItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MenuItemService menuItemService;

    @Test
    void createMenuItem_withValidData_returns201() throws Exception {
        UUID restaurantId = UUID.randomUUID();
        CreateMenuItemRequest request = new CreateMenuItemRequest();
        request.setName("Margherita Pizza");
        request.setPrice(new BigDecimal("12.99"));

        MenuItemResponse response = new MenuItemResponse();
        response.setId(UUID.randomUUID());
        response.setName("Margherita Pizza");
        response.setRestaurantId(restaurantId);

        when(menuItemService.createMenuItem(any(UUID.class), any(CreateMenuItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/restaurants/{restaurantId}/menu-items", restaurantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Margherita Pizza"));
    }

    @Test
    void createMenuItem_withNegativePrice_returns400() throws Exception {
        UUID restaurantId = UUID.randomUUID();
        CreateMenuItemRequest request = new CreateMenuItemRequest();
        request.setName("Broken Item");
        request.setPrice(new BigDecimal("-5.00"));

        mockMvc.perform(post("/restaurants/{restaurantId}/menu-items", restaurantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMenuByRestaurant_returnsOkWithList() throws Exception {
        UUID restaurantId = UUID.randomUUID();
        MenuItemResponse response = new MenuItemResponse();
        response.setName("Margherita Pizza");

        when(menuItemService.getMenuByRestaurantId(restaurantId)).thenReturn(List.of(response));

        mockMvc.perform(get("/menu").param("restaurantId", restaurantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Margherita Pizza"));
    }
}