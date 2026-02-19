package ru.cinimex.userservice.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.cinimex.userservice.domain.UserEntity;
import ru.cinimex.userservice.dto.UserResponseDto;
import ru.cinimex.userservice.exception.UserNotFoundException;
import ru.cinimex.userservice.mapper.UserMapper;
import ru.cinimex.userservice.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private JwtService jwtService;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private UserService userService;

    private UserEntity testUser;
    private UserResponseDto testResponseDto;
    private final String username = "ivan_ivanov";

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .username(username)
                .email("ivan@test.com")
                .build();

        testResponseDto = UserResponseDto.builder()
                .username(username)
                .email("ivan@test.com")
                .role("USER")
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Успешное получение пользователя по логину")
    void getUserByLogin_Success() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponseDto(testUser)).thenReturn(testResponseDto);

        UserResponseDto result = userService.getUserByLogin(username);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Ошибка: пользователь по логину не найден")
    void getUserByLogin_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserByLogin(username));
    }

    @Test
    @DisplayName("Успешное получение текущего пользователя из контекста")
    void getCurrentUser_Success() {
        // Настраиваем имитацию SecurityContextHolder
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(username);
        SecurityContextHolder.setContext(securityContext);

        // Настраиваем поведение для внутренней логики (getUserByLogin)
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponseDto(testUser)).thenReturn(testResponseDto);

        UserResponseDto result = userService.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        verify(authentication).getName();
    }

    @Test
    @DisplayName("Ошибка: текущий пользователь в контексте не найден в базе")
    void getCurrentUser_NotFoundInDb_ThrowsException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(username);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getCurrentUser());
    }

    @Test
    @DisplayName("Генерация технического токена")
    void generateTechToken_Success() {
        OffsetDateTime expire = OffsetDateTime.now().plusDays(1);
        String expectedToken = "tech-token-123";

        when(jwtService.generateTechToken(expire)).thenReturn(expectedToken);

        String result = userService.generateTechToken(expire);

        assertThat(result).isEqualTo(expectedToken);
        verify(jwtService).generateTechToken(expire);
    }
}