package ru.cinimex.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import ru.cinimex.userservice.dto.*;
import ru.cinimex.userservice.service.AuthService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerApi {

    private final AuthService authService;

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<JwtResponseDto> login(LoginRequestDto dto) {
        String token = authService.login(dto);
        return ResponseEntity.ok(new JwtResponseDto(token));
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserIdResponseDto> register(UserRegisterDto dto) {
        UUID userId = authService.register(dto);
        return ResponseEntity.ok(new UserIdResponseDto(userId));
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> confirmCode(CodeConfirmDto dto) {
        authService.confirmRegistration(dto);
        return ResponseEntity.ok().build();
    }
}