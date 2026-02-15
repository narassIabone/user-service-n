package ru.cinimex.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.cinimex.userservice.dto.CodeConfirmDto;
import ru.cinimex.userservice.dto.LoginRequestDto;
import ru.cinimex.userservice.dto.UserRegisterDto;
import ru.cinimex.userservice.service.AuthService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<String> login(LoginRequestDto dto) {
        String token = authService.login(dto);
        return ResponseEntity.ok(token);
    }

    @Override
    public ResponseEntity<UUID> register(UserRegisterDto dto) {
        return ResponseEntity.ok(authService.register(dto));
    }

    @Override
    public ResponseEntity<Void> confirmCode(CodeConfirmDto dto) {
        authService.confirmRegistration(dto);
        return ResponseEntity.ok().build();
    }
}