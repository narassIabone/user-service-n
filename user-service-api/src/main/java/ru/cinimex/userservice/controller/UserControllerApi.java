package ru.cinimex.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.cinimex.userservice.dto.TechTokenReponse;
import ru.cinimex.userservice.dto.TokenRequest;
import ru.cinimex.userservice.dto.UserResponseDto;

@Tag(name = "Пользователи", description = "Управление пользователями и технические токены")
public interface UserControllerApi {

    @Operation(summary = "Получить информацию о текущем пользователе")
    @GetMapping("/users/current")
    ResponseEntity<UserResponseDto> getCurrentUser();

    @Operation(summary = "Получить информацию о пользователе по логину (Админ)")
    @GetMapping("/admin/users/{login}")
    ResponseEntity<UserResponseDto> getUserByLogin(@PathVariable String login);

    @Operation(summary = "Сгенерировать технический токен (Админ)")
    @PostMapping("/admin/tech/token")
    ResponseEntity<TechTokenReponse> generateTechToken(@RequestBody TokenRequest request);
}
