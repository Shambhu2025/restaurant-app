package com.impactics.restaurant_app.service;

import com.impactics.restaurant_app.dto.CreateUserRequest;
import com.impactics.restaurant_app.dto.UserResponse;
import com.impactics.restaurant_app.entity.User;
import com.impactics.restaurant_app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_hashesPasswordAndSavesUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("john@example.com");
        request.setPassword("plainTextPassword");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhone("1234567890");

        when(passwordEncoder.encode("plainTextPassword")).thenReturn("hashedPasswordValue");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        UserResponse result = userService.createUser(request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        // Verify the password was actually hashed before saving, and the raw password never reaches the repository
        verify(passwordEncoder, times(1)).encode("plainTextPassword");

        verify(userRepository).save(argThat(user ->
                user.getPasswordHash().equals("hashedPasswordValue")
        ));
    }
}