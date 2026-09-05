package com.impactics.restaurant_app.service;

import com.impactics.restaurant_app.dto.CreateMenuItemRequest;
import com.impactics.restaurant_app.dto.MenuItemResponse;
import com.impactics.restaurant_app.entity.MenuItem;
import com.impactics.restaurant_app.entity.Restaurant;
import com.impactics.restaurant_app.exception.ResourceNotFoundException;
import com.impactics.restaurant_app.repository.MenuItemRepository;
import com.impactics.restaurant_app.repository.RestaurantRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private MenuItemService menuItemService;

    private Restaurant restaurant;
    private MenuItem menuItem;
    private UUID restaurantId;
    private UUID menuItemId;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        menuItemId = UUID.randomUUID();

        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Pizza Palace");

        menuItem = new MenuItem();
        menuItem.setId(menuItemId);
        menuItem.setRestaurant(restaurant);
        menuItem.setName("Margherita Pizza");
        menuItem.setDescription("Classic cheese and tomato");
        menuItem.setPrice(new BigDecimal("12.99"));
        menuItem.setCalories(800);
        menuItem.setIsAvailable(true);
    }

    @Test
    void getMenuByRestaurantId_whenRestaurantExists_returnsMenuItems() {
        when(restaurantRepository.existsById(restaurantId)).thenReturn(true);
        when(menuItemRepository.findByRestaurantId(restaurantId)).thenReturn(List.of(menuItem));

        List<MenuItemResponse> result = menuItemService.getMenuByRestaurantId(restaurantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Margherita Pizza");
    }

    @Test
    void getMenuByRestaurantId_whenRestaurantNotFound_throwsException() {
        UUID missingId = UUID.randomUUID();
        when(restaurantRepository.existsById(missingId)).thenReturn(false);

        assertThatThrownBy(() -> menuItemService.getMenuByRestaurantId(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(missingId.toString());

        verify(menuItemRepository, never()).findByRestaurantId(any());
    }

    @Test
    void createMenuItem_whenRestaurantExists_savesAndReturnsMenuItem() {
        CreateMenuItemRequest request = new CreateMenuItemRequest();
        request.setName("Pepperoni Pizza");
        request.setDescription("Spicy pepperoni");
        request.setPrice(new BigDecimal("14.99"));
        request.setCalories(900);

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        MenuItemResponse result = menuItemService.createMenuItem(restaurantId, request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Pepperoni Pizza");
        assertThat(result.getRestaurantId()).isEqualTo(restaurantId);
        assertThat(result.getIsAvailable()).isTrue();
    }

    @Test
    void createMenuItem_whenRestaurantNotFound_throwsException() {
        UUID missingId = UUID.randomUUID();
        CreateMenuItemRequest request = new CreateMenuItemRequest();
        request.setName("Test Item");
        request.setPrice(new BigDecimal("5.00"));

        when(restaurantRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.createMenuItem(missingId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(menuItemRepository, never()).save(any());
    }
}