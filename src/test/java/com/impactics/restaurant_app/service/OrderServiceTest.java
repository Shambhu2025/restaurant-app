package com.impactics.restaurant_app.service;

import com.impactics.restaurant_app.dto.CreateOrderRequest;
import com.impactics.restaurant_app.dto.OrderItemRequest;
import com.impactics.restaurant_app.dto.OrderResponse;
import com.impactics.restaurant_app.entity.*;
import com.impactics.restaurant_app.exception.ResourceNotFoundException;
import com.impactics.restaurant_app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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
    private OrderService orderService;

    private User user;
    private Restaurant restaurant;
    private MenuItem menuItem;
    private UUID userId;
    private UUID restaurantId;
    private UUID menuItemId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        menuItemId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setEmail("john@example.com");

        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Pizza Palace");
        restaurant.setDeliveryFee(new BigDecimal("2.99"));

        menuItem = new MenuItem();
        menuItem.setId(menuItemId);
        menuItem.setName("Margherita Pizza");
        menuItem.setPrice(new BigDecimal("12.99"));
    }

    private CreateOrderRequest buildValidOrderRequest(int quantity) {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setMenuItemId(menuItemId);
        itemRequest.setQuantity(quantity);
        itemRequest.setSpecialInstructions("Extra cheese");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(userId);
        request.setRestaurantId(restaurantId);
        request.setItems(List.of(itemRequest));
        request.setDeliveryAddress("123 Main St");
        request.setDeliveryNotes("Ring doorbell");
        return request;
    }

    @Test
    void createOrder_calculatesCorrectTotalsAndSavesOrder() {
        CreateOrderRequest request = buildValidOrderRequest(2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));

        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        when(orderItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = orderService.createOrder(request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getSubtotal()).isEqualByComparingTo("25.98"); // 12.99 * 2
        assertThat(result.getDeliveryFee()).isEqualByComparingTo("2.99");
        assertThat(result.getTotal()).isEqualByComparingTo("28.97");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getMenuItemName()).isEqualTo("Margherita Pizza");
        assertThat(result.getItems().get(0).getTotalPrice()).isEqualByComparingTo("25.98");
    }

    @Test
    void createOrder_whenRestaurantHasNoDeliveryFee_defaultsToZero() {
        restaurant.setDeliveryFee(null);
        CreateOrderRequest request = buildValidOrderRequest(1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = orderService.createOrder(request);

        assertThat(result.getDeliveryFee()).isEqualByComparingTo("0");
        assertThat(result.getTotal()).isEqualByComparingTo(result.getSubtotal());
    }

    @Test
    void createOrder_whenUserNotFound_throwsException() {
        CreateOrderRequest request = buildValidOrderRequest(1);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());

        verify(restaurantRepository, never()).findById(any());
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void createOrder_whenRestaurantNotFound_throwsException() {
        CreateOrderRequest request = buildValidOrderRequest(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(restaurantId.toString());

        verify(menuItemRepository, never()).findById(any());
    }

    @Test
    void createOrder_whenMenuItemNotFound_throwsException() {
        CreateOrderRequest request = buildValidOrderRequest(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(menuItemId.toString());

        verify(orderRepository, never()).saveAndFlush(any());
    }
}