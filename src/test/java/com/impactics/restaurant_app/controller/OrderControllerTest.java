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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

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
    void createOrder_withValidData_returns202WithLocationHeader() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = new OrderResponse();
        response.setId(orderId);
        response.setStatus("PENDING");

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", containsString("/order/" + orderId)))
                .andExpect(jsonPath("$.status").value("PENDING"));
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
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getOrderById_whenExists_returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = new OrderResponse();
        response.setId(orderId);
        response.setStatus("CONFIRMED");
        response.setTotal(new BigDecimal("28.97"));

        when(orderService.getOrderById(orderId)).thenReturn(response);

        mockMvc.perform(get("/order/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.total").value(28.97));
    }

    @Test
    void getOrderById_whenNotFound_returns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getOrderById(orderId))
                .thenThrow(new ResourceNotFoundException("Order not found with id: " + orderId));

        mockMvc.perform(get("/order/{id}", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_whenUnexpectedErrorOccurs_returns500WithoutLeakingDetails() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new RuntimeException("connection to db-internal-host failed"));

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("db-internal-host"))));
    }

    @Test
        void getOrderById_withMalformedId_returns400() throws Exception {
        mockMvc.perform(get("/order/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("orderId")));
        }

        @Test
        void deleteOrder_returns405WithAllowHeader() throws Exception {
        mockMvc.perform(delete("/order/{id}", UUID.randomUUID()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"));
        }

        @Test
        void createOrder_withMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void createOrder_withUnsupportedContentType_returns415() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        void unknownPath_returns404() throws Exception {
        mockMvc.perform(get("/this-endpoint-does-not-exist"))
                .andExpect(status().isNotFound());
        }
}