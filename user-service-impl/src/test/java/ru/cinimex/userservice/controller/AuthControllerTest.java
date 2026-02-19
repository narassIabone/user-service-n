package ru.cinimex.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.cinimex.userservice.dto.CodeConfirmDto;
import ru.cinimex.userservice.dto.LoginRequestDto;
import ru.cinimex.userservice.dto.UserRegisterDto;
import ru.cinimex.userservice.exception.InvalidCodeException;
import ru.cinimex.userservice.exception.UserAlreadyExistsException;
import ru.cinimex.userservice.exception.UserNotFoundException;
import ru.cinimex.userservice.service.AuthService;
import ru.cinimex.userservice.service.JwtService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(com.fasterxml.jackson.databind.ObjectMapper.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Nested
    @DisplayName("Регистрация пользователя (POST /register)")
    class RegisterTests {

        @Test
        @WithMockUser
        @DisplayName("Успешная регистрация -> 200 OK")
        void register_Success() throws Exception {
            UUID generatedId = UUID.randomUUID();
            UserRegisterDto dto = new UserRegisterDto("ivan", "123", "ivan@gmail.com");

            when(authService.register(any(UserRegisterDto.class))).thenReturn(generatedId);

            mockMvc.perform(post("/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(generatedId.toString()));
        }

        @Test
        @WithMockUser
        @DisplayName("Пользователь уже существует -> 400 Bad Request")
        void register_AlreadyExists() throws Exception {
            UserRegisterDto dto = new UserRegisterDto("ivan", "123", "ivan@gmail.com");

            when(authService.register(any())).thenThrow(new UserAlreadyExistsException("User exists"));

            mockMvc.perform(post("/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Подтверждение кода (POST /register/code)")
    class ConfirmCodeTests {

        @Test
        @WithMockUser
        @DisplayName("Успешное подтверждение -> 200 OK")
        void confirmCode_Success() throws Exception {
            CodeConfirmDto dto = new CodeConfirmDto();
            dto.setId(UUID.randomUUID());
            dto.setCode("653218");

            mockMvc.perform(post("/register/code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Пользователь не найден -> 400 Bad Request")
        void confirmCode_UserNotFound() throws Exception {
            CodeConfirmDto dto = new CodeConfirmDto();
            dto.setId(UUID.randomUUID());
            dto.setCode("653218");

            doThrow(new UserNotFoundException("Not found")).when(authService).confirmRegistration(any());

            mockMvc.perform(post("/register/code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Пользователь уже активен -> 400 Bad Request")
        void confirmCode_UserAlreadyActive() throws Exception {
            CodeConfirmDto dto = new CodeConfirmDto();
            dto.setId(UUID.randomUUID());
            dto.setCode("123456");

            doThrow(new InvalidCodeException("Пользователь уже активирован"))
                    .when(authService).confirmRegistration(any());

            mockMvc.perform(post("/register/code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Пользователь уже активирован"));
        }

        @Test
        @WithMockUser
        @DisplayName("Код не найден или истек -> 400 Bad Request")
        void confirmCode_CodeInvalid() throws Exception {
            CodeConfirmDto dto = new CodeConfirmDto();
            dto.setId(UUID.randomUUID());
            dto.setCode("000000");

            doThrow(new InvalidCodeException("Код подтверждения не найден или истек"))
                    .when(authService).confirmRegistration(any());

            mockMvc.perform(post("/register/code")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Код подтверждения не найден или истек"));
        }
    }

    @Nested
    @DisplayName("Авторизация (POST /auth/login)")
    class LoginTests {

        @Test
        @WithMockUser
        @DisplayName("Успешный вход -> 200 OK")
        void login_Success() throws Exception {
            LoginRequestDto dto = new LoginRequestDto("ivan", "123");
            String fakeToken = "eyJhbGciOiJIUzI1NiJ9...";

            when(authService.login(any())).thenReturn(fakeToken);

            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(fakeToken));
        }

        @Test
        @WithMockUser
        @DisplayName("Неверные учетные данные -> 403 Forbidden")
        void login_WrongCredentials() throws Exception {
            LoginRequestDto dto = new LoginRequestDto("ivan", "wrong_pass");

            when(authService.login(any())).thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }
    }
}