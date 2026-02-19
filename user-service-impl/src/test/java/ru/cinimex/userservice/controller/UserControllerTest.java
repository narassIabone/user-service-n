package ru.cinimex.userservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ru.cinimex.userservice.dto.TokenRequest;
import ru.cinimex.userservice.dto.UserResponseDto;
import ru.cinimex.userservice.service.JwtService;
import ru.cinimex.userservice.service.UserService;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @Autowired
    private JacksonTester<TokenRequest> tokenRequestJson;

    // Вспомогательный метод для создания заголовка
    private String getAuthHeader(String username, String role) {
        String token = jwtService.generateTestToken(username, role, OffsetDateTime.now().plusHours(1));
        return "Bearer " + token;
    }

    @Nested
    @DisplayName("GET /users/current")
    class CurrentUserTests {

        @Test
        @DisplayName("200 - Успех для USER")
        void userSuccess() throws Exception {
            when(userService.getCurrentUser()).thenReturn(new UserResponseDto("ivan", "ivan@gmail.com", "USER"));

            mockMvc.perform(get("/users/current")
                            .header("Authorization", getAuthHeader("ivan", "USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("ivan"))
                    .andExpect(jsonPath("$.email").value("ivan@gmail.com"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("200 - Успех для ADMIN")
        void adminSuccess() throws Exception {
            when(userService.getCurrentUser()).thenReturn(new UserResponseDto("ADMIN", "ADMIN@cinimex.ru", "ADMIN"));

            mockMvc.perform(get("/users/current")
                            .header("Authorization", getAuthHeader("ADMIN", "ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.email").value("ADMIN@cinimex.ru"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @DisplayName("403 - Запрещено для TECH")
        void techUnsuccess() throws Exception {
            when(userService.getCurrentUser()).thenReturn(new UserResponseDto("TECH", "TECH@cinimex.ru", "TECH"));

            mockMvc.perform(get("/users/current")
                            .header("Authorization", getAuthHeader("TECH", "TECH")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 - Без токена")
        void unauthorized() throws Exception {
            mockMvc.perform(get("/users/current"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("500 Internal Server Error - Ошибка логики")
        void serverError() throws Exception {
            // Имитируем падение сервиса
            when(userService.getCurrentUser())
                    .thenThrow(new RuntimeException("Unexpected database error"));

            mockMvc.perform(get("/users/current")
                            .header("Authorization", getAuthHeader("anyUser", "USER")))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Unexpected database error")));
        }
    }

    // --- 2. GET /users/{login} ---
    @Nested
    @DisplayName("GET /admin/users/{login}")
    class GetUserByLoginTests {

        private final String targetLogin = "targetUser";

        @Test
        @DisplayName("200 OK - Доступ разрешен для ADMIN")
        void adminCanSeeUser() throws Exception {
            UserResponseDto expectedUser = new UserResponseDto(targetLogin, "target@test.com", "USER");
            when(userService.getUserByLogin(targetLogin)).thenReturn(expectedUser);

            mockMvc.perform(get("/admin/users/" + targetLogin)
                            .header("Authorization", getAuthHeader("adminUser", "ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(targetLogin))
                    .andExpect(jsonPath("$.email").value("target@test.com"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("200 OK - Доступ разрешен для TECH")
        void techCanSeeUser() throws Exception {
            UserResponseDto expectedUser = new UserResponseDto(targetLogin, "tech-target@test.com", "USER");
            when(userService.getUserByLogin(targetLogin)).thenReturn(expectedUser);

            mockMvc.perform(get("/admin/users/" + targetLogin)
                            .header("Authorization", getAuthHeader("techService", "TECH")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(targetLogin));
        }

        @Test
        @DisplayName("403 Forbidden - Обычный USER не имеет доступа")
        void userCannotSeeOtherUser() throws Exception {
            mockMvc.perform(get("/admin/users/" + targetLogin)
                            .header("Authorization", getAuthHeader("simpleUser", "USER")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 Unauthorized - Токен не передан")
        void unauthorizedWithoutToken() throws Exception {
            mockMvc.perform(get("/admin/users/" + targetLogin))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("500 Internal Server Error - Ошибка в сервисе")
        void serverError() throws Exception {
            // Имитируем непредвиденную ошибку
            when(userService.getUserByLogin(targetLogin))
                    .thenThrow(new RuntimeException("Database connection failed"));

            mockMvc.perform(get("/admin/users/" + targetLogin)
                            .header("Authorization", getAuthHeader("adminUser", "ADMIN")))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /admin/tech/token")
    class GenerateTechTokenTests {

        private final String url = "/admin/tech/token";

        @Test
        @DisplayName("200 OK - Успех для ADMIN")
        void adminSuccess() throws Exception {
            OffsetDateTime expiry = OffsetDateTime.now().plusDays(1);
            TokenRequest request = new TokenRequest(expiry);
            String expectedToken = "generated_tech_token_string";

            when(userService.generateTechToken(any(OffsetDateTime.class))).thenReturn(expectedToken);

            mockMvc.perform(post("/admin/tech/token")
                            .header("Authorization", getAuthHeader("adminUser", "ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tokenRequestJson.write(request).getJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(expectedToken));
        }

        @Test
        @DisplayName("403 Forbidden - TECH не может генерировать токены")
        void techForbidden() throws Exception {
            TokenRequest request = new TokenRequest(OffsetDateTime.now().plusDays(1));

            mockMvc.perform(post(url)
                            .header("Authorization", getAuthHeader("techUser", "TECH"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tokenRequestJson.write(request).getJson()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("403 Forbidden - USER не может генерировать токены")
        void userForbidden() throws Exception {
            TokenRequest request = new TokenRequest(OffsetDateTime.now().plusDays(1));

            mockMvc.perform(post(url)
                            .header("Authorization", getAuthHeader("USER", "USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tokenRequestJson.write(request).getJson()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 Unauthorized - Без токена")
        void unauthorized() throws Exception {
            TokenRequest request = new TokenRequest(OffsetDateTime.now().plusDays(1));

            mockMvc.perform(post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tokenRequestJson.write(request).getJson()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("500 Internal Server Error - Ошибка при генерации")
        void serverError() throws Exception {
            OffsetDateTime expiry = OffsetDateTime.now().plusDays(1);
            TokenRequest request = new TokenRequest(expiry);

            when(userService.generateTechToken(any())).thenThrow(new RuntimeException("JWT signing error"));

            mockMvc.perform(post(url)
                            .header("Authorization", getAuthHeader("admin", "ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(tokenRequestJson.write(request).getJson()))
                    .andExpect(status().isInternalServerError());
        }
    }
}