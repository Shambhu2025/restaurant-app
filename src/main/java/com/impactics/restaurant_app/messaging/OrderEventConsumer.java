package com.impactics.restaurant_app.messaging;

import com.impactics.restaurant_app.config.KafkaConfig;
import com.impactics.restaurant_app.dto.OrderEvent;
import com.impactics.restaurant_app.dto.OrderItemRequest;
import com.impactics.restaurant_app.entity.MenuItem;
import com.impactics.restaurant_app.entity.Order;
import com.impactics.restaurant_app.entity.OrderItem;
import com.impactics.restaurant_app.entity.Restaurant;
import com.impactics.restaurant_app.entity.User;
import com.impactics.restaurant_app.repository.MenuItemRepository;
import com.impactics.restaurant_app.repository.OrderItemRepository;
import com.impactics.restaurant_app.repository.OrderRepository;
import com.impactics.restaurant_app.repository.RestaurantRepository;
import com.impactics.restaurant_app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public OrderEventConsumer(OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               UserRepository userRepository,
                               RestaurantRepository restaurantRepository,
                               MenuItemRepository menuItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @KafkaListener(topics = KafkaConfig.ORDER_EVENTS_TOPIC)
    @Transactional
    public void consume(OrderEvent event) {
        log.info("Received order event for orderId={}", event.getOrderId());
        

        Optional<Order> orderOpt = orderRepository.findById(event.getOrderId());
        if (orderOpt.isEmpty()) {
            log.error("Order {} referenced by Kafka message does not exist in the database - skipping", event.getOrderId());
            return;
        }

        Order order = orderOpt.get();

        // Idempotency guard: if this order has already moved past PENDING,
        // it's already been processed (e.g. a redelivered message) - skip silently.
        if (!"PENDING".equals(order.getStatus())) {
            log.info("Order {} already processed (status={}) - skipping duplicate message", order.getId(), order.getStatus());
            return;
        }

        Optional<User> userOpt = userRepository.findById(event.getUserId());
        if (userOpt.isEmpty() || !"ACTIVE".equals(userOpt.get().getStatus())) {
            failOrder(order, "User is not active or does not exist");
            return;
        }

        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(event.getRestaurantId());
        if (restaurantOpt.isEmpty() || !Boolean.TRUE.equals(restaurantOpt.get().getIsActive())) {
            failOrder(order, "Restaurant is not active or does not exist");
            return;
        }

        Restaurant restaurant = restaurantOpt.get();
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : event.getItems()) {
            Optional<MenuItem> menuItemOpt = menuItemRepository.findById(itemRequest.getMenuItemId());

            if (menuItemOpt.isEmpty() || !Boolean.TRUE.equals(menuItemOpt.get().getIsAvailable())) {
                failOrder(order, "Menu item not available: " + itemRequest.getMenuItemId());
                return;
            }

            

            MenuItem menuItem = menuItemOpt.get();

            if (!menuItem.getRestaurant().getId().equals(restaurant.getId())) {
                failOrder(order, "Menu item " + itemRequest.getMenuItemId() + " does not belong to restaurant " + restaurant.getId());
                return;
            }

            BigDecimal unitPrice = menuItem.getPrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            subtotal = subtotal.add(totalPrice);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setTotalPrice(totalPrice);
            orderItem.setSpecialInstructions(itemRequest.getSpecialInstructions());
            orderItems.add(orderItem);
        }

        BigDecimal deliveryFee = restaurant.getDeliveryFee() != null ? restaurant.getDeliveryFee() : BigDecimal.ZERO;

        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setTotal(subtotal.add(deliveryFee));
        order.setStatus("CONFIRMED");

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);

        log.info("Order {} confirmed successfully - total={}", order.getId(), order.getTotal());
    }

    private void failOrder(Order order, String reason) {
        order.setStatus("FAILED");
        order.setFailureReason(reason);
        orderRepository.save(order);
        log.warn("Order {} marked as FAILED: {}", order.getId(), reason);
    }
}