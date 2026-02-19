package ru.cinimex.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import ru.cinimex.userservice.dto.TechTokenReponse;
import ru.cinimex.userservice.dto.TokenRequest;
import ru.cinimex.userservice.dto.UserResponseDto;
import ru.cinimex.userservice.service.UserService;

@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerApi {

    private final UserService userService;

    @Override
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<UserResponseDto> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @Override
    @PreAuthorize("hasAnyAuthority('TECH', 'ADMIN')")
    public ResponseEntity<UserResponseDto> getUserByLogin(String login) {
        return ResponseEntity.ok(userService.getUserByLogin(login));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TechTokenReponse> generateTechToken(TokenRequest request) {
        String token = userService.generateTechToken(request.getExpiredDate());
        return ResponseEntity.ok(new TechTokenReponse(token));
    }
}