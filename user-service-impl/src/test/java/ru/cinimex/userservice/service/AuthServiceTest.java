package ru.cinimex.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.cinimex.userservice.domain.TempCodeEntity;
import ru.cinimex.userservice.domain.UserEntity;
import ru.cinimex.userservice.dto.CodeConfirmDto;
import ru.cinimex.userservice.dto.LoginRequestDto;
import ru.cinimex.userservice.dto.UserRegisterDto;
import ru.cinimex.userservice.exception.InvalidCodeException;
import ru.cinimex.userservice.exception.UserAlreadyExistsException;
import ru.cinimex.userservice.exception.UserNotFoundException;
import ru.cinimex.userservice.mapper.UserMapper;
import ru.cinimex.userservice.repository.TempCodeRepository;
import ru.cinimex.userservice.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TempCodeRepository tempCodeRepository;
    @Mock private UserMapper userMapper;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks
    private AuthService authService;

    private UserRegisterDto registerDto;
    private UserEntity userEntity;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        registerDto = new UserRegisterDto("testUser", "password123", "test@mail.ru");
        userEntity = UserEntity.builder()
                .id(userId)
                .username("testUser")
                .email("test@mail.ru")
                .build();
    }

    @Nested
    @DisplayName("Метод register")
    class RegisterTests {

        @Test
        @DisplayName("Успешная регистрация")
        void register_Success() {
            // Имитируем поведение TransactionTemplate: он просто запускает callback
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });

            when(userRepository.existsByUsernameOrEmail(any(), any())).thenReturn(false);
            when(userMapper.toEntity(any())).thenReturn(userEntity);

            UUID resultId = authService.register(registerDto);

            assertThat(resultId).isEqualTo(userId);
            verify(userRepository).save(userEntity);
            verify(tempCodeRepository).save(any(TempCodeEntity.class));
            // verify(kafkaTemplate).send(eq("notification.message.in"), any());
        }

        @Test
        @DisplayName("Ошибка: пользователь уже существует")
        void register_UserExists_ThrowsException() {
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(userRepository.existsByUsernameOrEmail(any(), any())).thenReturn(true);

            assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerDto));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Метод confirmRegistration")
    class ConfirmRegistrationTests {

        private CodeConfirmDto confirmDto;
        private TempCodeEntity tempCode;

        @BeforeEach
        void setUp() {
            confirmDto = new CodeConfirmDto();
            confirmDto.setId(userId);
            confirmDto.setCode("123456");

            tempCode = TempCodeEntity.builder()
                    .userId(userId)
                    .code("123456")
                    .build();
        }

        @Test
        @DisplayName("Успешное подтверждение")
        void confirm_Success() {
            userEntity.setActive(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(tempCodeRepository.findByUserId(userId)).thenReturn(Optional.of(tempCode));

            authService.confirmRegistration(confirmDto);

            assertThat(userEntity.isActive()).isTrue();
            verify(tempCodeRepository).delete(tempCode);
            verify(userRepository).save(userEntity);
        }

        @Test
        @DisplayName("Ошибка: пользователь уже активирован")
        void confirm_UserAlreadyActive_ThrowsException() {
            userEntity.setActive(true);

            when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            InvalidCodeException exception = assertThrows(InvalidCodeException.class,
                    () -> authService.confirmRegistration(confirmDto));

            assertThat(exception.getMessage()).isEqualTo("Пользователь уже активирован");

            verify(tempCodeRepository, never()).findByUserId(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ошибка: неверный код")
        void confirm_WrongCode_ThrowsException() {
            tempCode.setCode("000000"); // В базе один код, в DTO "123456"
            when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(tempCodeRepository.findByUserId(userId)).thenReturn(Optional.of(tempCode));

            assertThrows(InvalidCodeException.class, () -> authService.confirmRegistration(confirmDto));
        }

        @Test
        @DisplayName("Ошибка: пользователь не найден")
        void confirm_UserNotFound_ThrowsException() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> authService.confirmRegistration(confirmDto));
        }
    }

    @Nested
    @DisplayName("Метод login")
    class LoginTests {

        @Test
        @DisplayName("Успешный вход")
        void login_Success() {
            LoginRequestDto loginDto = new LoginRequestDto("user", "pass");
            Authentication auth = mock(Authentication.class);
            String expectedToken = "jwt.token.here";

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);
            when(jwtService.generateToken(auth)).thenReturn(expectedToken);

            String token = authService.login(loginDto);

            assertThat(token).isEqualTo(expectedToken);
            verify(authenticationManager).authenticate(any());
        }

        @Test
        @DisplayName("Ошибка: неверные учетные данные")
        void login_InvalidCredentials_ThrowsException() {
            LoginRequestDto loginDto = new LoginRequestDto("wrongUser", "wrongPass");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

            assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                    () -> authService.login(loginDto));

            verify(jwtService, never()).generateToken(any());
        }
    }
}