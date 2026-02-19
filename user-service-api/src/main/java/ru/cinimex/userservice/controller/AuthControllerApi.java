package ru.cinimex.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.cinimex.userservice.dto.*;

@Tag(name = "Аутентификация", description = "Методы для регистрации и входа")
public interface AuthControllerApi {

    @Operation(summary = "Регистрация нового пользователя")
    @PostMapping("/register")
    ResponseEntity<UserIdResponseDto> register(@Valid @RequestBody UserRegisterDto dto);

    @Operation(summary = "Подтверждение почты кодом")
    @PostMapping("/register/code")
    ResponseEntity<Void> confirmCode(@Valid @RequestBody CodeConfirmDto dto);

    @Operation(summary = "Аутентификация (вход)")
    @PostMapping("/auth/login")
    ResponseEntity<JwtResponseDto> login(@Valid @RequestBody LoginRequestDto dto);
}