package com.impactics.restaurant_app.controller;

import com.impactics.restaurant_app.dto.CreateOrderRequest;
import com.impactics.restaurant_app.dto.OrderResponse;
import com.impactics.restaurant_app.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/order")
@Tag(name = "Order Management", description = "Place orders and check their processing status")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(
            summary = "Place an order (asynchronous)",
            description = "Validates that the user and restaurant exist, saves the order with status PENDING, "
                    + "and publishes it to a Kafka topic for background processing. Returns immediately with "
                    + "202 Accepted. Pricing, availability checks and confirmation happen asynchronously: "
                    + "the order later moves to CONFIRMED, or FAILED with a failureReason. "
                    + "Follow the Location header (GET /order/{id}) to check its status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Order accepted and queued; status is PENDING"),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g. missing userId or restaurantId, empty items)"),
            @ApiResponse(responseCode = "404", description = "User or restaurant not found")
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                                       UriComponentsBuilder uriBuilder) {
        OrderResponse created = orderService.createOrder(request);

        URI location = uriBuilder.path("/order/{id}").buildAndExpand(created.getId()).toUri();

        return ResponseEntity.accepted().location(location).body(created);
    }

    @GetMapping("/{orderId}")
    @Operation(
            summary = "Get an order and its processing status",
            description = "Returns the order. Status is PENDING while queued, CONFIRMED once processed "
                    + "(totals and items populated), or FAILED with a failureReason explaining why."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }
}