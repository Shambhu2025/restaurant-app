package com.impactics.restaurant_app.service;

import com.impactics.restaurant_app.dto.CreateRestaurantRequest;
import com.impactics.restaurant_app.dto.RestaurantResponse;
import com.impactics.restaurant_app.entity.Restaurant;
import com.impactics.restaurant_app.exception.ResourceNotFoundException;
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
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private RestaurantService restaurantService;

    private Restaurant restaurant;
    private UUID restaurantId;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Pizza Palace");
        restaurant.setDescription("Best pizza in town");
        restaurant.setCuisine("Italian");
        restaurant.setDeliveryFee(new BigDecimal("2.99"));
        restaurant.setMinOrderAmount(new BigDecimal("10.00"));
        restaurant.setIsActive(true);
    }

    @Test
    void getAllRestaurants_returnsListOfRestaurants() {
        when(restaurantRepository.findAll()).thenReturn(List.of(restaurant));

        List<RestaurantResponse> result = restaurantService.getAllRestaurants();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Pizza Palace");
        verify(restaurantRepository, times(1)).findAll();
    }

    @Test
    void getRestaurantById_whenExists_returnsRestaurant() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        RestaurantResponse result = restaurantService.getRestaurantById(restaurantId);

        assertThat(result.getId()).isEqualTo(restaurantId);
        assertThat(result.getName()).isEqualTo("Pizza Palace");
    }

    @Test
    void getRestaurantById_whenNotFound_throwsException() {
        UUID missingId = UUID.randomUUID();
        when(restaurantRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurantById(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(missingId.toString());
    }

    @Test
    void createRestaurant_savesAndReturnsRestaurant() {
        CreateRestaurantRequest request = new CreateRestaurantRequest();
        request.setName("Burger Barn");
        request.setDescription("Great burgers");
        request.setCuisine("American");
        request.setDeliveryFee(new BigDecimal("1.99"));
        request.setMinOrderAmount(new BigDecimal("8.00"));

        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        RestaurantResponse result = restaurantService.createRestaurant(request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Burger Barn");
        assertThat(result.getIsActive()).isTrue();
        verify(restaurantRepository, times(1)).save(any(Restaurant.class));
    }
}