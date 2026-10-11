package com.impactics.restaurant_app.service;

import com.impactics.restaurant_app.config.KafkaConfig;
import com.impactics.restaurant_app.dto.CreateOrderRequest;
import com.impactics.restaurant_app.dto.OrderEvent;
import com.impactics.restaurant_app.dto.OrderItemRequest;
import com.impactics.restaurant_app.dto.OrderResponse;
import com.impactics.restaurant_app.entity.MenuItem;
import com.impactics.restaurant_app.entity.Order;
import com.impactics.restaurant_app.entity.OrderItem;
import com.impactics.restaurant_app.entity.Restaurant;
import com.impactics.restaurant_app.entity.User;
import com.impactics.restaurant_app.exception.ResourceNotFoundException;
import com.impactics.restaurant_app.repository.OrderRepository;
import com.impactics.restaurant_app.repository.RestaurantRepository;
import com.impactics.restaurant_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private UUID restaurantId;
    private UUID menuItemId;
    private UUID orderId;
    private User user;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        menuItemId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setStatus("ACTIVE");

        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setIsActive(true);
    }

    private CreateOrderRequest buildRequest() {
        OrderItemRequest item = new OrderItemRequest();
        item.setMenuItemId(menuItemId);
        item.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(userId);
        request.setRestaurantId(restaurantId);
        request.setItems(List.of(item));
        request.setDeliveryAddress("123 Test St");
        request.setDeliveryNotes("Ring bell");
        return request;
    }

    @Test
    void createOrder_savesPendingOrderAndPublishesEvent() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(orderId);
            return saved;
        });

        OrderResponse response = orderService.createOrder(buildRequest());

        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRestaurantId()).isEqualTo(restaurantId);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveAndFlush(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo("PENDING");
        assertThat(savedOrder.getIdempotencyKey()).isNotNull();

        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(kafkaTemplate).send(eq(KafkaConfig.ORDER_EVENTS_TOPIC), eq(restaurantId.toString()), eventCaptor.capture());
        OrderEvent event = eventCaptor.getValue();

        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getRestaurantId()).isEqualTo(restaurantId);
        assertThat(event.getItems()).hasSize(1);
        assertThat(event.getDeliveryAddress()).isEqualTo("123 Test St");
        // The key stored on the order row must match the key carried by the message,
        // otherwise the consumer could not reliably detect duplicates.
        assertThat(event.getIdempotencyKey()).isEqualTo(savedOrder.getIdempotencyKey());
    }

    @Test
    void createOrder_whenUserNotFound_throwsAndPublishesNothing() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());

        verify(orderRepository, never()).saveAndFlush(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(OrderEvent.class));
    }

    @Test
    void createOrder_whenRestaurantNotFound_throwsAndPublishesNothing() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(restaurantId.toString());

        verify(orderRepository, never()).saveAndFlush(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(OrderEvent.class));
    }

    @Test
    void getOrderById_whenOrderHasNoItems_returnsEmptyItemList() {
        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);
        order.setRestaurant(restaurant);
        order.setStatus("PENDING");
        order.setOrderItems(null);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId);

        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void getOrderById_whenConfirmed_returnsItemsTotalsAndFailureReasonFields() {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(menuItemId);
        menuItem.setName("Margherita Pizza");

        OrderItem orderItem = new OrderItem();
        orderItem.setId(UUID.randomUUID());
        orderItem.setMenuItem(menuItem);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(new BigDecimal("12.99"));
        orderItem.setTotalPrice(new BigDecimal("25.98"));

        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);
        order.setRestaurant(restaurant);
        order.setStatus("CONFIRMED");
        order.setSubtotal(new BigDecimal("25.98"));
        order.setDeliveryFee(new BigDecimal("2.99"));
        order.setTotal(new BigDecimal("28.97"));
        order.setOrderItems(List.of(orderItem));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId);

        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getTotal()).isEqualByComparingTo("28.97");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getMenuItemName()).isEqualTo("Margherita Pizza");
        assertThat(response.getItems().get(0).getTotalPrice()).isEqualByComparingTo("25.98");
    }

    @Test
    void getOrderById_whenFailed_exposesFailureReason() {
        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);
        order.setRestaurant(restaurant);
        order.setStatus("FAILED");
        order.setFailureReason("Restaurant is not active or does not exist");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getFailureReason()).isEqualTo("Restaurant is not active or does not exist");
    }

    @Test
    void getOrderById_whenNotFound_throwsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(orderId.toString());
    }
}