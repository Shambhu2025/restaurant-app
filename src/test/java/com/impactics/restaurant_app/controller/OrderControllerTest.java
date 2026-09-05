package com.impactics.restaurant_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impactics.restaurant_app.dto.CreateOrderRequest;
import com.impactics.restaurant_app.dto.OrderItemRequest;
import com.impactics.restaurant_app.dto.OrderResponse;
import com.impactics.restaurant_app.exception.ResourceNotFoundException;
import com.impactics.restaurant_app.service.OrderService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrderService orderService;

    private CreateOrderRequest buildValidRequest() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setMenuItemId(UUID.randomUUID());
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(UUID.randomUUID());
        request.setRestaurantId(UUID.randomUUID());
        request.setItems(List.of(itemRequest));
        request.setDeliveryAddress("123 Main St");
        return request;
    }

    @Test
    void createOrder_withValidData_returns201() throws Exception {
        CreateOrderRequest request = buildValidRequest();

        OrderResponse response = new OrderResponse();
        response.setId(UUID.randomUUID());
        response.setStatus("PENDING");
        response.setSubtotal(new BigDecimal("25.98"));
        response.setTotal(new BigDecimal("28.97"));

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total").value(28.97));
    }

    @Test
    void createOrder_withMissingUserId_returns400() throws Exception {
        CreateOrderRequest request = buildValidRequest();
        request.setUserId(null);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_withEmptyItems_returns400() throws Exception {
        CreateOrderRequest request = buildValidRequest();
        request.setItems(List.of());

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_whenUserNotFound_returns404() throws Exception {
        CreateOrderRequest request = buildValidRequest();

        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}