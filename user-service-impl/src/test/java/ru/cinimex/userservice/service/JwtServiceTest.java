package ru.cinimex.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;

    private final String secret = "myVerySecretKeyForJwtGenerationPurposes1234567890";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Внедряем значение @Value вручную
        ReflectionTestUtils.setField(jwtService, "jwtSecret", secret);
    }

    @Test
    @DisplayName("Успешная генерация и парсинг токена")
    void generateAndExtractToken_Success() {
        // Создаем пользователя Spring Security
        User user = new User("test_user", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        String token = jwtService.generateToken(auth);

        assertThat(token).isNotNull();
        assertThat(jwtService.extractUserName(token)).isEqualTo("test_user");
        assertThat(jwtService.extractRole(token)).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Ошибка генерации: некорректный Principal")
    void generateToken_WrongPrincipal_ThrowsException() {
        // В Principal вместо объекта User передаем обычную строку
        Authentication auth = new UsernamePasswordAuthenticationToken("just_string", null);

        assertThrows(IllegalArgumentException.class, () -> jwtService.generateToken(auth));
    }

    @Test
    @DisplayName("Генерация технического токена")
    void generateTechToken_Success() {
        OffsetDateTime expiry = OffsetDateTime.now().plusHours(1);

        String token = jwtService.generateTechToken(expiry);

        assertThat(jwtService.extractUserName(token)).isEqualTo("tech_user");
        assertThat(jwtService.extractRole(token)).containsExactly("TECH");

        // Проверка даты истечения (с погрешностью в пару секунд)
        Date expirationDate = jwtService.extractExpiration(token);
        assertThat(expirationDate).isCloseTo(Date.from(expiry.toInstant()), 2000);
    }

    @Test
    @DisplayName("Проверка срока действия токена (Expired)")
    void isTokenExpired_ShouldReturnTrueForOldToken() {
        // Генерируем токен, который уже истек (минус 1 час)
        OffsetDateTime pastDate = OffsetDateTime.now().minusHours(1);
        String token = jwtService.generateTechToken(pastDate);

        // Библиотека jjwt выбросит ExpiredJwtException при попытке распарсить такой токен
        // так как метод extractAllClaims делает верификацию
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> jwtService.isTokenExpired(token));
    }

    @Test
    @DisplayName("Извлечение ролей из токена")
    void extractRole_Success() {
        User user = new User("admin", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")));
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        String token = jwtService.generateToken(auth);
        List<String> roles = jwtService.extractRole(token);

        assertThat(roles).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }
}