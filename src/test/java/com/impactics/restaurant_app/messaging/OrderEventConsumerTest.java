package com.impactics.restaurant_app.messaging;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private OrderEventConsumer consumer;

    @Captor
    private ArgumentCaptor<List<OrderItem>> itemsCaptor;

    private UUID orderId;
    private UUID userId;
    private UUID restaurantId;
    private UUID menuItemId;

    private Order order;
    private User user;
    private Restaurant restaurant;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        menuItemId = UUID.randomUUID();

        order = new Order();
        order.setId(orderId);
        order.setStatus("PENDING");

        user = new User();
        user.setId(userId);
        user.setStatus("ACTIVE");

        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setIsActive(true);
        restaurant.setDeliveryFee(new BigDecimal("2.99"));

        menuItem = new MenuItem();
        menuItem.setId(menuItemId);
        menuItem.setName("Margherita Pizza");
        menuItem.setPrice(new BigDecimal("12.99"));
        menuItem.setIsAvailable(true);
        menuItem.setRestaurant(restaurant);
    }

    private OrderEvent buildEvent(int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setMenuItemId(menuItemId);
        item.setQuantity(quantity);
        item.setSpecialInstructions("Extra cheese");

        OrderEvent event = new OrderEvent();
        event.setOrderId(orderId);
        event.setIdempotencyKey(UUID.randomUUID());
        event.setUserId(userId);
        event.setRestaurantId(restaurantId);
        event.setItems(List.of(item));
        return event;
    }

    @Test
    void consume_whenEverythingValid_confirmsOrderWithCorrectTotals() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));

        consumer.consume(buildEvent(2));

        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
        assertThat(order.getSubtotal()).isEqualByComparingTo("25.98");   // 12.99 * 2
        assertThat(order.getDeliveryFee()).isEqualByComparingTo("2.99");
        assertThat(order.getTotal()).isEqualByComparingTo("28.97");

        verify(orderRepository).save(order);
        verify(orderItemRepository).saveAll(itemsCaptor.capture());

        List<OrderItem> savedItems = itemsCaptor.getValue();
        assertThat(savedItems).hasSize(1);
        assertThat(savedItems.get(0).getQuantity()).isEqualTo(2);
        assertThat(savedItems.get(0).getUnitPrice()).isEqualByComparingTo("12.99");
        assertThat(savedItems.get(0).getTotalPrice()).isEqualByComparingTo("25.98");
        assertThat(savedItems.get(0).getOrder()).isSameAs(order);
        assertThat(savedItems.get(0).getSpecialInstructions()).isEqualTo("Extra cheese");
    }

    @Test
    void consume_whenRestaurantHasNoDeliveryFee_defaultsToZero() {
        restaurant.setDeliveryFee(null);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));

        consumer.consume(buildEvent(1));

        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
        assertThat(order.getDeliveryFee()).isEqualByComparingTo("0");
        assertThat(order.getTotal()).isEqualByComparingTo(order.getSubtotal());
    }

    @Test
    void consume_whenOrderDoesNotExist_skipsWithoutTouchingAnythingElse() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        consumer.consume(buildEvent(1));

        verifyNoInteractions(userRepository, restaurantRepository, menuItemRepository, orderItemRepository);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void consume_whenOrderAlreadyProcessed_skipsAsDuplicate() {
        order.setStatus("CONFIRMED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        consumer.consume(buildEvent(1));

        // Idempotency guard: nothing is re-validated, re-priced, or re-saved
        verifyNoInteractions(userRepository, restaurantRepository, menuItemRepository, orderItemRepository);
        verify(orderRepository, never()).save(any(Order.class));
        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void consume_whenUserInactive_failsOrder() {
        user.setStatus("INACTIVE");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        consumer.consume(buildEvent(1));

        assertThat(order.getStatus()).isEqualTo("FAILED");
        assertThat(order.getFailureReason()).contains("User");
        verify(orderRepository).save(order);
        verifyNoInteractions(restaurantRepository, menuItemRepository, orderItemRepository);
    }

    @Test
    void consume_whenUserMissing_failsOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        consumer.consume(buildEvent(1));

        assertThat(order.getStatus()).isEqualTo("FAILED");
        assertThat(order.getFailureReason()).contains("User");
        verifyNoInteractions(restaurantRepository, menuItemRepository, orderItemRepository);
    }

    @Test
    void consume_whenRestaurantInactive_failsOrder() {
        restaurant.setIsActive(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        consumer.consume(buildEvent(1));

        assertThat(order.getStatus()).isEqualTo("FAILED");
        assertThat(order.getFailureReason()).contains("Restaurant");
        verifyNoInteractions(menuItemRepository, orderItemRepository);
    }

    @Test
    void consume_whenRestaurantMissing_failsOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        consumer.consume(buildEvent(1));

        assertThat(order.getStatus()).isEqualTo("FAILED");
        assertThat(order.getFailureReason()).contains("Restaurant");
        verifyNoInteractions(menuItemRepository, orderItemRepository);
    }

    @Test
    void consume_whenMenuItemUnavailable_failsOrder() {
        menuItem.setIsAvailable(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));

        consumer.consume(buildEvent(1));

        assertThat(order.getStatus()).isEqualTo("FAILED");
        assertThat(order.getFailureReason()).contains("not available");
        verify(orderItemRepository, never()).saveAll(anyList());
    }

    @Test
    void consume_whenMenuItemMissing_failsOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.empty());

        consumer.consume(buildEvent(1));

        assertThat(order.getStatus()).isEqualTo("FAILED");
        assertThat(order.getFailureReason()).contains("not available");
        verify(orderItemRepository, never()).saveAll(anyList());
    }

    @Test
    void consume_whenMenuItemBelongsToAnotherRestaurant_failsOrder() {
        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setId(UUID.randomUUID());
        menuItem.setRestaurant(otherRestaurant);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));

        consumer.consume(buildEvent(1));

        assertThat(order.getStatus()).isEqualTo("FAILED");
        assertThat(order.getFailureReason()).contains("does not belong to restaurant");
        verify(orderItemRepository, never()).saveAll(anyList());
    }
}