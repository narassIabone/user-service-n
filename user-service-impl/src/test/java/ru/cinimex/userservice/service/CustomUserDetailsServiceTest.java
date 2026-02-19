package ru.cinimex.userservice.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.cinimex.userservice.domain.UserEntity;
import ru.cinimex.userservice.domain.UserRole;
import ru.cinimex.userservice.repository.UserRepository;


import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_Success() {
        // Данные
        UserEntity entity = new UserEntity();
        entity.setUsername("test-user");
        entity.setPassword("hash-pass");
        entity.setRole(UserRole.USER);
        entity.setActive(true);

        when(userRepository.findByUsername("test-user")).thenReturn(Optional.of(entity));

        // Вызов
        UserDetails userDetails = userDetailsService.loadUserByUsername("test-user");

        // Проверки
        assertEquals("test-user", userDetails.getUsername());
        assertEquals("hash-pass", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("USER")));
    }

    @Test
    void loadUserByUsername_UserDisabled() {
        UserEntity entity = new UserEntity();
        entity.setUsername("inactive-user");
        entity.setRole(UserRole.USER);
        entity.setActive(false); // Отключен

        when(userRepository.findByUsername("inactive-user")).thenReturn(Optional.of(entity));

        UserDetails userDetails = userDetailsService.loadUserByUsername("inactive-user");

        assertFalse(userDetails.isEnabled()); // Проверяем флаг disabled
    }

    @Test
    void loadUserByUsername_NotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("unknown");
        });
    }
}