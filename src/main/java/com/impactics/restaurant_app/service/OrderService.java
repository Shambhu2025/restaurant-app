package com.impactics.restaurant_app.service;

import com.impactics.restaurant_app.config.KafkaConfig;
import com.impactics.restaurant_app.dto.CreateOrderRequest;
import com.impactics.restaurant_app.dto.OrderEvent;
import com.impactics.restaurant_app.dto.OrderItemResponse;
import com.impactics.restaurant_app.dto.OrderResponse;
import com.impactics.restaurant_app.entity.Order;
import com.impactics.restaurant_app.entity.OrderItem;
import com.impactics.restaurant_app.entity.Restaurant;
import com.impactics.restaurant_app.entity.User;
import com.impactics.restaurant_app.exception.ResourceNotFoundException;
import com.impactics.restaurant_app.repository.OrderRepository;
import com.impactics.restaurant_app.repository.RestaurantRepository;
import com.impactics.restaurant_app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderService(OrderRepository orderRepository,
                         UserRepository userRepository,
                         RestaurantRepository restaurantRepository,
                         KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        // Cheap referential-integrity checks only - NOT the full business validation
        // (active/available checks, price calculation). That happens asynchronously
        // in the consumer, once the message is picked up.
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + request.getRestaurantId()));

        UUID idempotencyKey = UUID.randomUUID();

        Order order = new Order();
        order.setUser(user);
        order.setRestaurant(restaurant);
        order.setStatus("PENDING");
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryNotes(request.getDeliveryNotes());
        order.setIdempotencyKey(idempotencyKey);

        Order savedOrder = orderRepository.saveAndFlush(order);

        OrderEvent event = new OrderEvent();
        event.setOrderId(savedOrder.getId());
        event.setIdempotencyKey(idempotencyKey);
        event.setUserId(request.getUserId());
        event.setRestaurantId(request.getRestaurantId());
        event.setItems(request.getItems());
        event.setDeliveryAddress(request.getDeliveryAddress());
        event.setDeliveryNotes(request.getDeliveryNotes());

        kafkaTemplate.send(KafkaConfig.ORDER_EVENTS_TOPIC, request.getRestaurantId().toString(), event);

        log.info("Order {} queued for processing (idempotencyKey={})", savedOrder.getId(), idempotencyKey);

        return toResponse(savedOrder, Collections.emptyList());
    }

    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        List<OrderItemResponse> itemResponses = order.getOrderItems() == null
                ? Collections.emptyList()
                : order.getOrderItems().stream().map(this::toItemResponse).collect(Collectors.toList());

        return toResponse(order, itemResponses);
    }

    private OrderResponse toResponse(Order order, List<OrderItemResponse> items) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUser().getId());
        response.setRestaurantId(order.getRestaurant().getId());
        response.setSubtotal(order.getSubtotal());
        response.setDeliveryFee(order.getDeliveryFee());
        response.setTotal(order.getTotal());
        response.setDeliveryAddress(order.getDeliveryAddress());
        response.setDeliveryNotes(order.getDeliveryNotes());
        response.setStatus(order.getStatus());
        response.setFailureReason(order.getFailureReason());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setItems(items);
        return response;
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setMenuItemId(item.getMenuItem().getId());
        response.setMenuItemName(item.getMenuItem().getName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setTotalPrice(item.getTotalPrice());
        response.setSpecialInstructions(item.getSpecialInstructions());
        return response;
    }
}